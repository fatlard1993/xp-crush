package justfatlard.xp_crush.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/** Bottles o' enchanting on an anvil, against a mending sword and a plain one. */
public final class AnvilBottles implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			ServerPlayer[] holder = new ServerPlayer[1];
			server.runOnServer(s -> holder[0] = world.getConnection().getServerPlayer());

			String report = server.computeOnServer(s -> {
				ServerPlayer player = holder[0];
				var level = player.level();
				BlockPos pos = player.blockPosition().above(3);
				level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
				var mending = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
				StringBuilder out = new StringBuilder();

				// 100 damage, 3 bottles at 7 points each, 2 durability a point: 42 mended.
				ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
				sword.enchant(mending, 1);
				sword.setDamageValue(100);
				AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(level, pos));
				menu.getSlot(0).set(sword);
				menu.getSlot(1).set(new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
				ItemStack result = menu.getSlot(2).getItem();
				check(out, "partial repair damage", result.getDamageValue(), 58);
				check(out, "partial repair cost", menu.getCost(), 1);
				check(out, "prior work untouched", result.getOrDefault(DataComponents.REPAIR_COST, 0), 0);

				// Plenty of bottles: only as many as the damage needs are taken.
				player.setExperienceLevels(5);
				menu.getSlot(1).set(new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
				result = menu.getSlot(2).getItem();
				check(out, "full repair damage", result.getDamageValue(), 0);
				menu.getSlot(2).onTake(player, result.copy());
				check(out, "bottles left after full repair", menu.getSlot(1).getItem().getCount(), 64 - 8);
				check(out, "levels after full repair", player.experienceLevel, 4);
				menu.removed(player);

				// No mending: the bottles do nothing.
				ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
				plain.setDamageValue(100);
				AnvilMenu other = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(level, pos));
				other.getSlot(0).set(plain);
				other.getSlot(1).set(new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
				check(out, "no mending, no result", other.getSlot(2).getItem().isEmpty() ? 1 : 0, 1);
				other.removed(player);
				return out.toString();
			});
			System.out.println("ANVIL BOTTLES\n" + report);
			if (report.contains("FAIL")) throw new AssertionError("anvil bottles:\n" + report);
		}
	}

	private static void check(StringBuilder out, String name, int got, int want) {
		out.append(got == want ? "  ok   " : "  FAIL ").append(name).append(": ").append(got)
			.append(got == want ? "" : " (wanted " + want + ")").append('\n');
	}
}
