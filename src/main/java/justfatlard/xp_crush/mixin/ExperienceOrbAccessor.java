package justfatlard.xp_crush.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An orb's value is per orb, and several can be riding in one entity. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
	@Accessor("count") int xpCrush$count();
}
