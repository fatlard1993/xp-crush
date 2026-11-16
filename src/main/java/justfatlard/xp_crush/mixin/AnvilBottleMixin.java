package justfatlard.xp_crush.mixin;

import justfatlard.xp_crush.Mending;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil's result when the right slot holds experience bottles and the left something that
 * mends. Vanilla's {@code onTake} then charges the levels, uses up {@code repairItemCountCost}
 * bottles and wears the anvil, as for any other repair material.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilBottleMixin extends ItemCombinerMenu {
	@Shadow @Final private DataSlot cost;
	@Shadow private int repairItemCountCost;
	@Shadow private String itemName;
	@Shadow private boolean onlyRenaming;

	private AnvilBottleMixin(MenuType<?> type, int id, ItemCombinerMenuSlotDefinition slots) {
		super(type, id, null, null, slots);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void xpCrush$bottleRepair(CallbackInfo ci) {
		if (!(player.level() instanceof ServerLevel level)) return;
		ItemStack input = inputSlots.getItem(0);
		Mending.Repair repair = Mending.repair(level, input, inputSlots.getItem(1));
		if (repair == null) return;

		// One level for the anvil's work, and the prior-work penalty left where it was: the
		// bottles are the payment, and mending exists so a tool never has to climb that ladder.
		ItemStack result = repair.result();
		int price = 1;
		if (itemName != null && !StringUtil.isBlank(itemName)) {
			if (!itemName.equals(input.getHoverName().getString())) {
				price++;
				result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
			}
		} else if (input.has(DataComponents.CUSTOM_NAME)) {
			price++;
			result.remove(DataComponents.CUSTOM_NAME);
		}

		onlyRenaming = false;
		repairItemCountCost = repair.bottles();
		cost.set(price);
		resultSlots.setItem(0, result);
		broadcastChanges();
		ci.cancel();
	}
}
