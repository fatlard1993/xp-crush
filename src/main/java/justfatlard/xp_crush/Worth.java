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
 * <p>Built once per server start and again on reload, since that is when recipes change.
 */
public final class Worth {
	private Worth() {}

	/** A recipe that makes {@code count} of something from one item chosen out of each option list. */
	private record Producer(List<List<Item>> ingredients, int count) {}

	private static final Map<Item, List<Producer>> producers = new HashMap<>();
	private static final Map<Item, Double> prices = new HashMap<>();

	/** How long a chain of recipes is followed before the thing at the end is called raw. */
	private static final int MAX_DEPTH = 64;

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

		Main.LOGGER.info("[{}] Priced {} recipes for {} items", Main.MOD_ID, counted, producers.size());
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
		return compute(item, new HashSet<>(), 0, new boolean[1]);
	}

	/**
	 * The cheapest way to have one of these.
	 *
	 * <p>A recipe that needs the thing it makes - nuggets from an ingot from nuggets - is walked
	 * once and then refused, so the loop bottoms out on whichever side has a price of its own.
	 *
	 * <p>A price found without refusing anything is the item's price and is kept. One found while
	 * part of the tree was off limits is only right for the walk that found it, so it is used and
	 * forgotten: the same item asked about from the top gets the whole tree and the true price.
	 *
	 * @param tainted set when any refusal or cut-off shaped the answer
	 */
	private static double compute(Item item, Set<Item> walking, int depth, boolean[] tainted) {
		Double configured = XpCrushConfig.priceOf(BuiltInRegistries.ITEM.getKey(item));
		if (configured != null) return configured;

		Double known = prices.get(item);
		if (known != null) return known;

		List<Producer> made = producers.get(item);
		if (made == null || made.isEmpty()) {
			prices.put(item, XpCrushConfig.xpPerRawItem());
			return XpCrushConfig.xpPerRawItem();
		}
		if (depth >= MAX_DEPTH || !walking.add(item)) {
			tainted[0] = true;
			return XpCrushConfig.xpPerRawItem();
		}

		boolean[] here = new boolean[1];
		double best = Double.POSITIVE_INFINITY;
		for (Producer producer : made) {
			double cost = 0.0;
			boolean possible = true;
			for (List<Item> options : producer.ingredients()) {
				double cheapest = Double.POSITIVE_INFINITY;
				for (Item option : options) {
					if (walking.contains(option)) {
						here[0] = true;
						continue;
					}
					cheapest = Math.min(cheapest, compute(option, walking, depth + 1, here));
				}
				if (cheapest == Double.POSITIVE_INFINITY) {
					possible = false;
					break;
				}
				cost += cheapest;
			}
			if (!possible) continue;
			best = Math.min(best, cost / producer.count() + XpCrushConfig.xpPerCraftStep());
		}
		walking.remove(item);

		double price = best == Double.POSITIVE_INFINITY ? XpCrushConfig.xpPerRawItem() : best;
		if (!here[0]) prices.put(item, price);
		tainted[0] |= here[0];
		return price;
	}
}
