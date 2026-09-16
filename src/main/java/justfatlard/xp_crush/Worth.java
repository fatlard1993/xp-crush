package justfatlard.xp_crush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * What an item is worth, in experience.
 *
 * <p>Worked out from the recipe tree rather than written down per item, because the tree already
 * knows. A crafted thing is worth its ingredients plus a little for the crafting, divided by how
 * many the recipe makes; where several recipes make it, the cheapest one is the price, since that
 * is the one a player would have used. The bottom of the tree is a price list of raw materials
 * from the config, and anything raw that is not on the list gets the price of a lump of
 * cobblestone.
 *
 * <p>On top of the item's own price: its rarity multiplies it, every enchantment level adds a
 * flat amount, a worn tool pays for what is left of it, and a box pays for what is inside.
 *
 * <p>Built once per server start and again on reload, since that is when recipes change, and
 * built whole: every price is settled then, so a crush is a lookup. It used to be worked out on
 * demand by walking the tree from the item down, and a walk through the tree's loops - nuggets
 * and ingots and blocks, every dye, every stage of copper - could not keep what it found, so the
 * same branches were walked again from every direction. One crushed item took the server's tick
 * past sixty seconds and the watchdog shut the server down.
 */
public final class Worth {
	private Worth() {}

	/** A recipe that makes {@code count} of something from one item chosen out of each option list. */
	private record Producer(List<List<Item>> ingredients, int count) {}

	private static final Map<Item, List<Producer>> producers = new HashMap<>();
	private static final Map<Item, Double> prices = new HashMap<>();

	public static void rebuild(MinecraftServer server) {
		producers.clear();
		prices.clear();

		ContextMap context = SlotDisplayContext.fromLevel(server.overworld());
		int counted = 0;

		for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
			Recipe<?> recipe = holder.value();
			// A special recipe makes something out of what you put in, and its worth is the
			// worth of what you put in. A transmute is the same item wearing a different coat,
			// and a trim is the same armour wearing a stripe.
			if (recipe.isSpecial() || recipe instanceof TransmuteRecipe || recipe instanceof SmithingTrimRecipe) continue;

			PlacementInfo placement = recipe.placementInfo();
			if (placement.isImpossibleToPlace()) continue;

			List<List<Item>> ingredients = new ArrayList<>();
			for (Ingredient ingredient : placement.ingredients()) {
				List<Item> options = ingredient.items().map(Holder::value).toList();
				if (!options.isEmpty()) ingredients.add(options);
			}
			if (ingredients.isEmpty()) continue;

			for (RecipeDisplay display : recipe.display()) {
				ItemStack result = display.result().resolveForFirstStack(context);
				if (result.isEmpty()) continue;
				producers.computeIfAbsent(result.getItem(), item -> new ArrayList<>())
					.add(new Producer(ingredients, Math.max(1, result.getCount())));
				counted++;
			}
		}

		settle();
		Main.LOGGER.info("[{}] Priced {} recipes for {} items", Main.MOD_ID, counted, producers.size());
	}

	/**
	 * Every price, cheapest first, the way Dijkstra settles distances.
	 *
	 * <p>Raw materials and configured prices go in first. The cheapest unsettled item is settled,
	 * and each recipe it can fill a slot in hears about it; a recipe whose every slot is filled
	 * offers its result a price. Because things are settled in order of price, the first option
	 * to fill a slot is the cheapest option that slot will ever have, so each slot is filled once
	 * and each recipe costed once. A loop costs nothing extra: an item on one is settled by
	 * whichever way into it is cheapest and the way back round is never better, since every
	 * craft adds to the price rather than taking from it.
	 *
	 * <p>Anything never reached is made only from things that are made only from each other, with
	 * no raw material anywhere under it. It gets the raw price, as it did before.
	 */
	private static void settle() {
		// Keyed by identity wherever a recipe is the key: two recipes with the same ingredients and
		// count are equal records - stone into stairs and stone into bricks at the stonecutter - and
		// are still two recipes making two things.
		// Which recipe slots each item can fill.
		record Slot(Producer producer, int index) {}
		Map<Item, List<Slot>> fills = new HashMap<>();
		Map<Producer, Item> resultOf = new java.util.IdentityHashMap<>();
		for (Map.Entry<Item, List<Producer>> entry : producers.entrySet()) {
			for (Producer producer : entry.getValue()) {
				resultOf.put(producer, entry.getKey());
				for (int i = 0; i < producer.ingredients().size(); i++) {
					for (Item option : producer.ingredients().get(i)) {
						fills.computeIfAbsent(option, k -> new ArrayList<>()).add(new Slot(producer, i));
					}
				}
			}
		}

		record Offer(Item item, double price) {}
		java.util.PriorityQueue<Offer> queue =
			new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(Offer::price));
		Map<Item, Double> best = new HashMap<>();

		Set<Item> everything = new HashSet<>(producers.keySet());
		everything.addAll(fills.keySet());
		for (Item item : everything) {
			Double configured = XpCrushConfig.priceOf(BuiltInRegistries.ITEM.getKey(item));
			double start = configured != null ? configured
				: producers.containsKey(item) ? Double.POSITIVE_INFINITY
				: XpCrushConfig.xpPerRawItem();
			if (start < Double.POSITIVE_INFINITY) {
				best.put(item, start);
				queue.add(new Offer(item, start));
			}
		}

		Map<Producer, boolean[]> filled = new java.util.IdentityHashMap<>();
		Map<Producer, double[]> sums = new java.util.IdentityHashMap<>();
		Map<Producer, int[]> remaining = new java.util.IdentityHashMap<>();

		while (!queue.isEmpty()) {
			Offer offer = queue.poll();
			if (prices.containsKey(offer.item()) || offer.price() > best.get(offer.item())) continue;
			prices.put(offer.item(), offer.price());

			for (Slot slot : fills.getOrDefault(offer.item(), List.of())) {
				Producer producer = slot.producer();
				boolean[] done = filled.computeIfAbsent(producer, k -> new boolean[k.ingredients().size()]);
				if (done[slot.index()]) continue;
				done[slot.index()] = true;
				double[] sum = sums.computeIfAbsent(producer, k -> new double[1]);
				sum[0] += offer.price();
				int[] left = remaining.computeIfAbsent(producer, k -> new int[] {k.ingredients().size()});
				if (--left[0] > 0) continue;

				Item result = resultOf.get(producer);
				// A configured price is the price; recipes do not argue with it.
				if (XpCrushConfig.priceOf(BuiltInRegistries.ITEM.getKey(result)) != null) continue;
				double price = sum[0] / producer.count() + XpCrushConfig.xpPerCraftStep();
				if (!prices.containsKey(result) && price < best.getOrDefault(result, Double.POSITIVE_INFINITY)) {
					best.put(result, price);
					queue.add(new Offer(result, price));
				}
			}
		}

		for (Item item : producers.keySet()) prices.putIfAbsent(item, XpCrushConfig.xpPerRawItem());
	}

	/** The experience this stack pays when crushed. */
	public static double of(ItemStack stack) {
		if (stack.isEmpty()) return 0.0;

		double each = priceOf(stack.getItem()) * rarityOf(stack.getItem());

		// What is left of it. A pickaxe on its last swing was mostly spent already.
		if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
			each *= Math.max(0.0, (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage());
		}

		double total = each * stack.getCount();

		total += levelsOn(stack.get(DataComponents.ENCHANTMENTS)) * XpCrushConfig.xpPerEnchantmentLevel();
		total += levelsOn(stack.get(DataComponents.STORED_ENCHANTMENTS)) * XpCrushConfig.xpPerEnchantmentLevel();

		// A box is worth what is in it, crushed along with it.
		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		if (container != null) {
			for (ItemStackTemplate inside : container.nonEmptyItems()) total += of(inside.create());
		}
		BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (bundle != null) {
			for (ItemStackTemplate inside : bundle.items()) total += of(inside.create());
		}

		return total;
	}

	private static int levelsOn(ItemEnchantments enchantments) {
		if (enchantments == null || enchantments.isEmpty()) return 0;
		int levels = 0;
		for (var entry : enchantments.entrySet()) levels += entry.getIntValue();
		return levels;
	}

	/** The item's own rarity, before an enchantment bumps it: the enchantments are paid for separately. */
	private static double rarityOf(Item item) {
		Rarity rarity = new ItemStack(item).getRarity();
		if (rarity == Rarity.UNCOMMON) return XpCrushConfig.rarityUncommon();
		if (rarity == Rarity.RARE) return XpCrushConfig.rarityRare();
		if (rarity == Rarity.EPIC) return XpCrushConfig.rarityEpic();
		return 1.0;
	}

	/** One of the item, plain, in experience. */
	public static double priceOf(Item item) {
		Double known = prices.get(item);
		if (known != null) return known;
		// In no recipe at all, either way round: raw, or priced in the config.
		Double configured = XpCrushConfig.priceOf(BuiltInRegistries.ITEM.getKey(item));
		return configured != null ? configured : XpCrushConfig.xpPerRawItem();
	}
}
