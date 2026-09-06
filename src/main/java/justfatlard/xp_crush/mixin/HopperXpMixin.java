package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.Bottling;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The experience a hopper is holding between bottles, saved with the hopper. */
@Mixin(HopperBlockEntity.class)
public abstract class HopperXpMixin implements Bottling.Holds {

	@Unique private static final String STORED_KEY = "xp_crush:stored";

	@Unique private int xpCrush$stored;

	@Override
	public int xpCrush$stored() {
		return xpCrush$stored;
	}

	@Override
	public void xpCrush$store(int value) {
		xpCrush$stored = Math.max(0, value);
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void xpCrush$save(ValueOutput output, CallbackInfo ci) {
		if (xpCrush$stored > 0) output.putInt(STORED_KEY, xpCrush$stored);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void xpCrush$load(ValueInput input, CallbackInfo ci) {
		xpCrush$stored = input.getIntOr(STORED_KEY, 0);
	}
}
