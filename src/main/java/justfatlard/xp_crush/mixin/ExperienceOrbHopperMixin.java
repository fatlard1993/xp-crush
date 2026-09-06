package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.Bottling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each orb, after it has moved for the tick, is offered to whatever hopper it has landed in. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbHopperMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void xpCrush$offerToHopper(CallbackInfo ci) {
		ExperienceOrb orb = (ExperienceOrb) (Object) this;
		if (orb.isRemoved() || !(orb.level() instanceof ServerLevel level)) return;
		Bottling.offer(level, orb);
	}
}
