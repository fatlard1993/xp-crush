package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.Crush;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The moment a falling block lands.
 *
 * <p>This is where vanilla hurts whatever an anvil landed on, and it only ever looks at living
 * things. The items are still where they were - the block is placed a moment later in the same
 * tick - so this is the last moment they can be caught under it.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockCrushMixin {

	@Shadow private boolean hurtEntities;

	@Inject(method = "causeFallDamage", at = @At("HEAD"))
	private void xpCrush$landed(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		FallingBlockEntity block = (FallingBlockEntity) (Object) this;
		if (!(block.level() instanceof ServerLevel server)) return;
		Crush.byFallingBlock(server, block, hurtEntities, fallDistance);
	}
}
