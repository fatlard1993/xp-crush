package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.Crush;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After a moving piston block has shoved everything in its way.
 *
 * <p>Vanilla moves each entity out of the block's path by however much it can and then stops
 * caring: a mob left inside a wall suffocates on its own, and an item left inside a wall just sits
 * there. This is the moment to look at what is left.
 */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonCrushMixin {

	@Inject(method = "moveCollidedEntities", at = @At("RETURN"))
	private static void xpCrush$afterShove(Level level, BlockPos pos, float progress, PistonMovingBlockEntity moving, CallbackInfo ci) {
		if (!(level instanceof ServerLevel server)) return;
		Crush.byPiston(server, pos, moving.getMovementDirection());
	}
}
