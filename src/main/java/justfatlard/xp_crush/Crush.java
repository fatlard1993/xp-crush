package justfatlard.xp_crush;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

/**
 * The crushing itself: which items are caught, and what they turn into.
 *
 * <p>Vanilla has no notion of an item being crushed. A piston shoves items along and, when they
 * cannot go further, leaves them inside the block; an anvil falls through them without noticing;
 * sand fills the hole and the item climbs out. So each of these is a judgement call made here,
 * and the judgement is the same in every case: the item is somewhere an item cannot be, because
 * something moved into where it was.
 */
public final class Crush {
	private Crush() {}

	/**
	 * How far inside a block an item has to be to count.
	 *
	 * <p>An item pushed cleanly along a floor ends the tick exactly against the face that pushed
	 * it, and an item lying on the ground can sit a hair into it. Neither is crushed. One that a
	 * piston has driven into a wall is a good part of a block deep, because the piston moved half
	 * a block this tick and the item went nowhere.
	 */
	private static final double DEEP = 0.1;

	/** How far ahead of an item the way has to be clear for it to count as merely pushed. */
	private static final double AHEAD = 0.3;

	/**
	 * After a piston has moved everything it can: whatever is left inside something solid, and
	 * whatever is caught between the mover and something solid ahead of it.
	 *
	 * <p>The second is the piston head on its own. Its plate is a quarter of a block, so an item
	 * driven ahead of it into a wall ends in the three-quarter pocket behind the plate, touching
	 * both and inside neither, and by the inside rule alone it just sat there. Pinned is crushed:
	 * touching the mover, with no room to go the way it is being pushed.
	 */
	public static void byPiston(ServerLevel level, BlockPos moving, Direction direction) {
		if (!XpCrushConfig.pistons()) return;

		// The block's destination and the one beyond it, which is where a wall would be.
		AABB region = new AABB(moving)
			.expandTowards(direction.getStepX(), direction.getStepY(), direction.getStepZ())
			.inflate(0.01);
		// Where the block being moved will stand when it arrives, not where it is now.
		//
		// Now is no use. This runs while the piston is still travelling, and by the time it
		// returns the item has already been shoved as far as it will go: flush against the wall,
		// a fifth of a block clear of a head that has not finished coming out. Neither inside
		// anything nor touching anything, so nothing counted it - and the tick the head does
		// land on it, nothing runs at all. The destination is the honest question: an item still
		// standing where the block is about to be is an item that could not get out of the way.
		AABB landing = null;
		if (level.getBlockEntity(moving) instanceof PistonMovingBlockEntity piston) {
			VoxelShape shape = piston.getMovedState().getCollisionShape(level, moving);
			if (!shape.isEmpty()) landing = shape.bounds().move(moving);
		}

		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, region)) {
			if (item.isRemoved()) continue;
			AABB box = item.getBoundingBox();
			boolean inside = !level.noCollision(item, box.deflate(DEEP));
			// Shrunk a little before it is slid ahead, so a box resting on a floor is not read as
			// colliding with the floor it rests on.
			boolean pinned = landing != null && box.intersects(landing.inflate(0.02))
				&& !level.noCollision(item, box.deflate(0.02)
				.move(direction.getStepX() * AHEAD, direction.getStepY() * AHEAD, direction.getStepZ() * AHEAD));
			if (!inside && !pinned) continue;
			Press press = pressing(level, item);
			crush(level, item, force(0.0, press.blocks(), press.squeezes()));
		}
	}

	/**
	 * What is pressing on an item: how many blocks in all, and on how many axes from both sides.
	 *
	 * @param blocks   every moving block touching the item plus everything moving behind each
	 * @param squeezes axes on which a moving block presses in from each end - a vice, not a wall
	 */
	private record Press(int blocks, int squeezes) {}

	/**
	 * Every moving block touching the item, and everything moving behind each of them.
	 *
	 * <p>A bare piston head pushing an item is one. A head shoving twelve blocks of stone ahead of
	 * it is thirteen, and the item at the far end is under all of them. Four heads closing on the
	 * same item from four sides are all of their lines added together, because the item is under
	 * all of those too. Each line is walked once, so a line that touches the item twice - front
	 * block and the one behind it, on a block already half inside - is not counted twice.
	 *
	 * <p>Two heads meeting from opposite sides are noted separately. Being driven into a wall and
	 * being caught between two things driving are different amounts of crushed, and the second
	 * is the one that took two pistons to arrange.
	 */
	private static Press pressing(ServerLevel level, ItemEntity item) {
		AABB touching = item.getBoundingBox().inflate(0.05);
		Set<BlockPos> counted = new HashSet<>();
		Set<Direction> pushing = EnumSet.noneOf(Direction.class);
		int blocks = 0;

		for (BlockPos pos : BlockPos.betweenClosed(
				BlockPos.containing(touching.minX, touching.minY, touching.minZ),
				BlockPos.containing(touching.maxX, touching.maxY, touching.maxZ))) {
			if (counted.contains(pos) || !level.getBlockState(pos).is(Blocks.MOVING_PISTON)) continue;
			if (!(level.getBlockEntity(pos) instanceof PistonMovingBlockEntity moving)) continue;

			Direction direction = moving.getMovementDirection();
			pushing.add(direction);
			BlockPos at = pos.immutable();
			int line = 0;
			while (line < 16 && level.getBlockState(at).is(Blocks.MOVING_PISTON) && counted.add(at)) {
				line++;
				at = at.relative(direction.getOpposite());
			}
			blocks += line;
		}

		int squeezes = 0;
		for (Direction.Axis axis : Direction.Axis.values()) {
			if (pushing.contains(Direction.get(Direction.AxisDirection.POSITIVE, axis))
					&& pushing.contains(Direction.get(Direction.AxisDirection.NEGATIVE, axis))) {
				squeezes++;
			}
		}
		return new Press(Math.max(1, blocks), squeezes);
	}

	/**
	 * A falling block has just landed, and is about to become a block where these items are.
	 *
	 * <p>One that hurts - an anvil, a stalactite - crushes on its own, on the same terms it hurts:
	 * a fall of more than one block. One that does not needs company. A lone block of sand landing
	 * on an item just pops the item out of the hole it filled, and so does the mod: it takes a
	 * column, {@code column_min} deep, to bury something.
	 */
	public static void byFallingBlock(ServerLevel level, FallingBlockEntity block, boolean hurts, double fallDistance) {
		if (fallDistance <= 1.0) return;

		int stacked = columnAbove(level, block);
		if (hurts) {
			if (!XpCrushConfig.fallingBlocks()) return;
		} else {
			if (!XpCrushConfig.fallingColumns()) return;
			if (stacked < XpCrushConfig.columnMin()) return;
		}

		double force = force(fallDistance, stacked, 0);
		BlockPos landing = block.blockPosition();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(landing))) {
			if (item.isRemoved()) continue;
			crush(level, item, force);
		}
	}

	/**
	 * How hard the crushing was, as a multiplier on what the item pays.
	 *
	 * <p>One for the least that counts: an anvil dropped from just over a block, a bare piston
	 * head. Every block of fall beyond that, every extra block in the stack or the chain, and
	 * every axis squeezed from both ends adds its share, up to a ceiling, because an anvil dropped
	 * from the build limit onto a stick is a stunt and not a business.
	 */
	private static double force(double fallDistance, int stacked, int squeezes) {
		double force = 1.0
			+ Math.max(0.0, fallDistance - 2.0) * XpCrushConfig.xpPerBlockFallen()
			+ Math.max(0, stacked - 1) * XpCrushConfig.xpPerBlockStacked()
			+ squeezes * XpCrushConfig.xpPerSqueeze();
		return Math.min(force, XpCrushConfig.forceCap());
	}

	/** This block and every other falling block stacked in the column above it. */
	private static int columnAbove(ServerLevel level, FallingBlockEntity block) {
		BlockPos landing = block.blockPosition();
		// Far enough up to count a tall column for the force, not only to reach the minimum.
		int reach = Math.max(XpCrushConfig.columnMin(), 16);
		AABB column = new AABB(landing.above()).expandTowards(0, reach, 0);
		int stacked = 1;
		for (FallingBlockEntity other : level.getEntitiesOfClass(FallingBlockEntity.class, column)) {
			if (other != block && !other.isRemoved()) stacked++;
		}
		return stacked;
	}

	private static void crush(ServerLevel level, ItemEntity item, double force) {
		ItemStack stack = item.getItem();
		if (stack.isEmpty()) return;

		double worth = Worth.of(stack) * force;
		Vec3 at = item.position();
		item.discard();

		// Fractions are paid by chance rather than dropped, so a stack of things worth a twentieth
		// each still pays a twentieth each over time.
		int xp = (int) Math.floor(worth);
		if (level.getRandom().nextDouble() < worth - xp) xp++;
		if (xp > 0) ExperienceOrb.award(level, at, xp);

		level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.4F);
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
			at.x, at.y + 0.1, at.z, 6 + Math.min(stack.getCount(), 16), 0.15, 0.1, 0.15, 0.05);
	}
}
