package justfatlard.xp_crush;

import justfatlard.xp_crush.mixin.ExperienceOrbAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hoppers that drink experience, so long as they have something to keep it in.
 *
 * <p>An orb that lands in a hopper is taken in and held as a number, and every time the number
 * reaches a bottle's worth, an empty glass bottle - from the hopper itself or from the container
 * it feeds - is filled and put back. A hopper with no empty bottle anywhere leaves the orbs alone,
 * because a hopper that swallowed experience it could not keep would be a hole in the floor.
 *
 * <p>The held number lives on the hopper and is saved with it, so a farm does not lose a part-filled
 * bottle to a restart.
 */
public final class Bottling {
	private Bottling() {}

	/** What a hopper remembers: experience taken in and not yet bottled. */
	public interface Holds {
		int xpCrush$stored();

		void xpCrush$store(int value);
	}

	/** An orb has ticked; if it is sitting in a hopper that can use it, take it. */
	public static void offer(ServerLevel level, ExperienceOrb orb) {
		if (!XpCrushConfig.hopperBottles()) return;

		BlockPos pos = orb.blockPosition();
		HopperBlockEntity hopper = hopperAt(level, pos);
		if (hopper == null) hopper = hopperAt(level, pos.below());
		if (hopper == null) return;

		BlockState state = level.getBlockState(hopper.getBlockPos());
		if (!state.getValue(HopperBlock.ENABLED)) return;

		Direction facing = state.getValue(HopperBlock.FACING);
		Container fed = HopperBlockEntity.getContainerAt(level, hopper.getBlockPos().relative(facing));

		if (findBottle(hopper) < 0 && (fed == null || findBottle(fed) < 0)) return;

		Holds holds = (Holds) hopper;
		int taken = orb.getValue() * ((ExperienceOrbAccessor) orb).xpCrush$count();
		holds.xpCrush$store(holds.xpCrush$stored() + taken);
		orb.discard();
		level.playSound(null, orb.getX(), orb.getY(), orb.getZ(),
			SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.3F, 0.7F + level.getRandom().nextFloat() * 0.3F);

		fill(level, hopper, fed, facing, holds);
	}

	/** Turn as much of the held experience into bottles as there are bottles to fill. */
	private static void fill(ServerLevel level, HopperBlockEntity hopper, Container fed, Direction facing, Holds holds) {
		int perBottle = Math.max(1, XpCrushConfig.xpPerBottle());
		boolean filled = false;

		while (holds.xpCrush$stored() >= perBottle) {
			Container from = findBottle(hopper) >= 0 ? hopper : fed != null && findBottle(fed) >= 0 ? fed : null;
			if (from == null) break;

			from.removeItem(findBottle(from), 1);
			from.setChanged();
			holds.xpCrush$store(holds.xpCrush$stored() - perBottle);

			// Back where the empty came from, by preference: bottles kept in the chest stay in
			// the chest. Then the hopper, then the chest anyway, and only then the floor.
			ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
			ItemStack left = HopperBlockEntity.addItem(hopper, from, bottle, from == fed ? facing.getOpposite() : null);
			if (!left.isEmpty()) left = HopperBlockEntity.addItem(hopper, hopper, left, null);
			if (!left.isEmpty() && fed != null && from != fed) {
				left = HopperBlockEntity.addItem(hopper, fed, left, facing.getOpposite());
			}
			if (!left.isEmpty()) {
				BlockPos above = hopper.getBlockPos().above();
				Block.popResource(level, above, left);
			}
			filled = true;
		}

		if (filled) {
			hopper.setChanged();
			BlockPos pos = hopper.getBlockPos();
			level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
		}
	}

	private static HopperBlockEntity hopperAt(ServerLevel level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof HopperBlockEntity hopper ? hopper : null;
	}

	/** The slot holding an empty glass bottle, or -1. */
	private static int findBottle(Container container) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).is(Items.GLASS_BOTTLE)) return slot;
		}
		return -1;
	}
}
