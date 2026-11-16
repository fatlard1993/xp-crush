package justfatlard.xp_crush;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Bottles of experience as the anvil's repair material for anything that mends.
 *
 * <p>A bottle holds what a hopper put into it, {@link XpCrushConfig#xpPerBottle()} points, and on
 * the anvil those points mend exactly as they would picked up as orbs: the item's own enchantments
 * decide how much durability a point buys. The anvil uses as many bottles as the damage needs and
 * no more.
 */
public final class Mending {
	private Mending() {}

	public record Repair(ItemStack result, int bottles) {}

	/** The repaired copy and the bottles it took, or null where this is not a bottle repair. */
	public static Repair repair(ServerLevel level, ItemStack input, ItemStack addition) {
		if (!XpCrushConfig.anvilBottles()) return null;
		if (!addition.is(Items.EXPERIENCE_BOTTLE)) return null;
		if (!input.isDamaged() || !EnchantmentHelper.has(input, EnchantmentEffectComponents.REPAIR_WITH_XP)) return null;

		ItemStack result = input.copy();
		int bottles = 0;
		while (result.isDamaged() && bottles < addition.getCount()) {
			int mended = EnchantmentHelper.modifyDurabilityToRepairFromXp(level, result, XpCrushConfig.xpPerBottle());
			if (mended <= 0) break;
			result.setDamageValue(Math.max(0, result.getDamageValue() - mended));
			bottles++;
		}
		return bottles == 0 ? null : new Repair(result, bottles);
	}
}
