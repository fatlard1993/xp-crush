package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.XpCrushConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Experience from a mob nobody killed.
 *
 * <p>Vanilla pays experience only for a death a player had a recent hand in: the fall, the lava,
 * the iron golem and the trapdoor all pay nothing. This pays a reduced share for those, on the
 * same terms vanilla applies otherwise - the mob has to be one that drops, and the game rule has
 * to be on. Deaths vanilla already pays for are left to vanilla, so nothing is paid twice.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityXpMixin {

	@Shadow protected int lastHurtByPlayerMemoryTime;

	@Shadow protected abstract boolean isAlwaysExperienceDropper();

	@Shadow public abstract boolean shouldDropExperience();

	@Shadow public abstract boolean wasExperienceConsumed();

	@Shadow public abstract int getExperienceReward(ServerLevel level, Entity killer);

	@Inject(method = "dropExperience", at = @At("HEAD"))
	private void xpCrush$unattended(ServerLevel level, Entity killer, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self instanceof Mob) || !XpCrushConfig.unattendedKills()) return;

		// Vanilla's own turn, or nothing to pay.
		if (wasExperienceConsumed() || isAlwaysExperienceDropper() || lastHurtByPlayerMemoryTime > 0) return;
		if (!shouldDropExperience() || !level.getGameRules().get(GameRules.MOB_DROPS)) return;

		double share = getExperienceReward(level, killer) * XpCrushConfig.unattendedKillFraction();
		int xp = (int) Math.floor(share);
		if (level.getRandom().nextDouble() < share - xp) xp++;
		if (xp > 0) ExperienceOrb.award(level, self.position(), xp);
	}
}
