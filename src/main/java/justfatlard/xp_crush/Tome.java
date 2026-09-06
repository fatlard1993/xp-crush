package justfatlard.xp_crush;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Experience, bound into a book.
 *
 * <p>Write {@code /xp 500} on the first line of a book and quill and set it on a lectern - or
 * {@code /xp all}, which binds as much as you have once the binding has taken its share. If you
 * have the five hundred points and the binding's share on top, the lectern takes them and hands
 * you back a tome that glows like an enchanted book and holds the five hundred; use it later, or
 * somebody else does, and they come out again. The binding is the cost of moving experience
 * around: without it a tome is a free bank, and with it every bottle is a small decision. If you
 * do not have enough, the book stays a book and gains a line explaining the sum and saying how
 * many you could have bound. If you wrote anything else, nothing happens at all, and the lectern
 * takes the book the way it takes any book.
 *
 * <p>Points and never levels. The ladder gets steeper as it climbs, so a level is worth forty
 * points near the bottom and over a hundred near the top; a tome denominated in levels could be
 * bound cheap and cashed dear, and the ladder's whole point is that the high rungs are the
 * expensive ones. A number of points is the same number wherever it is spent.
 *
 * <p>The binding's share climbs with the amount, the way the ladder does: a small tome pays a
 * small share and a hoard pays a large one, so binding everything into one book is not the cheap
 * way to carry it. And a number too small to be worth a book is turned away, because somebody
 * who writes {@code /xp 4} is nearly always thinking in levels, and a tome of four points would
 * teach them nothing except that it did not work.
 *
 * <p>The lectern is the altar and nothing more: the tome comes to your hand rather than staying on
 * the stand, because a lectern only knows how to show a book, and this is not one any more.
 */
public final class Tome {
	private Tome() {}

	private static final String POINTS_KEY = "xp_crush_points";
	/** The first tomes remembered levels. Read so one already in a chest still opens. */
	private static final String LEVELS_KEY = "xp_crush_levels";

	/** The spell, on the first line of the first page. Whatever follows it is ignored. */
	private static final Pattern SPELL = Pattern.compile("^\\s*/xp\\s+(\\d{1,9}|all)\\s*(?:\\r?\\n|$)", Pattern.CASE_INSENSITIVE);

	/** The line this mod writes back, so a second try does not stack a second one under it. */
	private static final String NOTE_START = "\n\nBinding ";

	/** A book and quill set on a lectern. */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide() || !XpCrushConfig.tomes()) return InteractionResult.PASS;

		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.WRITABLE_BOOK)) return InteractionResult.PASS;

		BlockPos pos = hit.getBlockPos();
		if (!(level.getBlockState(pos).getBlock() instanceof LecternBlock)) return InteractionResult.PASS;
		if (level.getBlockState(pos).getValue(LecternBlock.HAS_BOOK)) return InteractionResult.PASS;

		WritableBookContent content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
		if (content == null || content.pages().isEmpty()) return InteractionResult.PASS;

		String page = content.pages().get(0).raw();
		Matcher spell = SPELL.matcher(page);
		if (!spell.find()) return InteractionResult.PASS;

		if (!(player instanceof ServerPlayer server)) return InteractionResult.PASS;
		int held = pointsHeld(player);
		int minimum = XpCrushConfig.tomeMinimumPoints();

		// "all" is everything the binding leaves room for: the largest amount whose share still
		// fits inside what is held.
		boolean all = spell.group(1).equalsIgnoreCase("all");
		int points = all ? affordable(held) : Integer.parseInt(spell.group(1));
		if (points <= 0 && !all) return InteractionResult.PASS;

		// Written into the book rather than said, because the book is what they were looking at,
		// and it is what they will open again to fix the number.
		if (all && points < minimum) {
			annotate(stack, content, page, "You have " + held + (held == 1 ? " point" : " points")
				+ ", and a tome needs " + minimum + " plus the binding's share. Come back with more");
			return InteractionResult.PASS;
		}
		if (points < minimum) {
			int levels = levelsFromNothing(minimum);
			annotate(stack, content, page, "Binding counts points, not levels, and " + points
				+ (points == 1 ? " point" : " points") + " is not worth the paper. Try " + minimum
				+ " or more, which is about " + levels + " levels from nothing");
			return InteractionResult.PASS;
		}

		int fee = fee(points);
		int cost = points + fee;
		if (held < cost) {
			// The sum is spelled out, since the number they wrote is not the number they are short of.
			int spare = affordable(held);
			annotate(stack, content, page, "Binding " + points + " points costs " + cost + ", " + fee
				+ " for the binding itself. You have " + held
				+ (spare >= minimum ? ", try " + spare : ", not enough to bind any"));
			return InteractionResult.PASS;
		}

		// Emptied and refilled with what is left, so the ladder is climbed back up by the game's
		// own rule rather than by arithmetic that has to agree with it.
		server.setExperienceLevels(0);
		server.setExperiencePoints(0);
		server.giveExperiencePoints(held - cost);
		stack.shrink(1);

		ItemStack tome = make(points);
		if (!player.addItem(tome)) player.drop(tome, false, Prediction.SERVER_ONLY);

		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		player.sendSystemMessage(Component.literal(points + " points bound into a tome, "
			+ fee + " spent on the binding"));
		return InteractionResult.SUCCESS;
	}

	/** The binding's share of this many points, which grows as the pile does. */
	private static double share(int points) {
		double share = XpCrushConfig.tomeBindingShare()
			+ XpCrushConfig.tomeBindingSharePerThousand() * (points / 1000.0);
		return Math.min(share, XpCrushConfig.tomeBindingShareMax());
	}

	private static int fee(int points) {
		return (int) Math.ceil(points * share(points));
	}

	/** The most that can be bound out of this many points, fee included. */
	private static int affordable(int held) {
		int low = 0;
		int high = held;
		while (low < high) {
			int mid = (low + high + 1) / 2;
			if (mid + fee(mid) <= held) low = mid;
			else high = mid - 1;
		}
		return low;
	}

	/** How many levels a fresh player would climb with this many points, for saying so in the book. */
	private static int levelsFromNothing(int points) {
		int level = 0;
		while (points >= pointsToNext(level)) {
			points -= pointsToNext(level);
			level++;
		}
		return level;
	}

	/** A line under the spell, replacing any earlier line of ours, and the lectern takes the book. */
	private static void annotate(ItemStack stack, WritableBookContent content, String page, String note) {
		int old = page.indexOf(NOTE_START);
		String kept = old >= 0 ? page.substring(0, old) : page.stripTrailing();
		List<Filterable<String>> pages = new ArrayList<>(content.pages());
		pages.set(0, Filterable.passThrough(kept + "\n\n" + note));
		stack.set(DataComponents.WRITABLE_BOOK_CONTENT, content.withReplacedPages(pages));
	}

	/**
	 * Everything the player has, as points: the rungs climbed and the way up the current one.
	 *
	 * <p>Vanilla's ladder is the same three lines as the player's own answer, spelled out here
	 * because the player can only be asked about the level they are standing on.
	 */
	private static int pointsHeld(Player player) {
		int points = 0;
		for (int level = 0; level < player.experienceLevel; level++) points += pointsToNext(level);
		return points + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
	}

	private static int pointsToNext(int level) {
		if (level >= 30) return 112 + (level - 30) * 9;
		if (level >= 15) return 37 + (level - 15) * 5;
		return 7 + level * 2;
	}

	/** A tome, used: what it holds comes back out, as points. */
	public static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!isTome(stack)) return InteractionResult.PASS;

		if (!level.isClientSide()) {
			CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
			int points = tag.getIntOr(POINTS_KEY, 0);
			if (points > 0) player.giveExperiencePoints(points);
			else player.giveExperienceLevels(tag.getIntOr(LEVELS_KEY, 0));
			stack.shrink(1);
			level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	/** An enchanted book to look at, and a number inside. */
	public static ItemStack make(int points) {
		ItemStack tome = new ItemStack(Items.ENCHANTED_BOOK);

		CompoundTag tag = new CompoundTag();
		tag.putInt(POINTS_KEY, points);
		tome.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		tome.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		// Literal, not a key: this mod is server-side and the client has no lang file for it.
		tome.set(DataComponents.ITEM_NAME, Component.literal("Tome of Experience"));
		tome.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal(points + " points"),
			Component.literal("Use to take them back"))));
		return tome;
	}

	/** Whether this is a tome of ours, old or new. */
	public static boolean isTome(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return false;
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return false;
		CompoundTag tag = data.copyTag();
		return tag.getIntOr(POINTS_KEY, 0) > 0 || tag.getIntOr(LEVELS_KEY, 0) > 0;
	}
}
