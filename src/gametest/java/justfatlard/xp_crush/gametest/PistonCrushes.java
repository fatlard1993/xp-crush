package justfatlard.xp_crush.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** Pistons crushing items, laid out the ways players build them. */
public final class PistonCrushes implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getConnection().waitForChunksRender();
			server.runCommand("gamerule block_drops true");
			BlockPos origin = server.computeOnServer(s -> world.getConnection().getServerPlayer().blockPosition());
			int x = origin.getX() + 20;
			int y = 100;
			int z = origin.getZ();
			StringBuilder report = new StringBuilder();

			// Each layout on a platform of its own, 8 apart.
			report.append(layout(context, server, "bare head into a wall", x, y, z, false, false, false));
			report.append(layout(context, server, "sticky with glass into a wall", x, y, z + 8, true, true, false));
			report.append(layout(context, server, "sticky with glass, pulled back and pushed again", x, y, z + 16, true, true, true));
			report.append(layout(context, server, "sticky with glass down onto the floor", x, y, z + 24, true, true, false, true));
			System.out.println("PISTONS\n" + report);
			if (report.toString().contains("SURVIVED")) throw new AssertionError("items survived:\n" + report);
		}
	}

	private static String layout(ClientGameTestContext context, TestServerContext server, String name, int x, int y, int z,
			boolean sticky, boolean glass, boolean cycle) {
		return layout(context, server, name, x, y, z, sticky, glass, cycle, false);
	}

	/**
	 * A piston facing east (or down), a glass block on its face if asked, an item lying where the
	 * glass is pushed to, and stone beyond it. Powered by a redstone block set behind it.
	 */
	private static String layout(ClientGameTestContext context, TestServerContext server, String name, int x, int y, int z,
			boolean sticky, boolean glass, boolean cycle, boolean down) {
		String piston = sticky ? "sticky_piston" : "piston";
		server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 2, y - 1, z - 2, x + 6, y - 1, z + 2));
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 2, y, z - 2, x + 6, y + 4, z + 2));
		int itemX;
		int itemY;
		String powerAt;
		if (!down) {
			server.runCommand("setblock %d %d %d minecraft:%s[facing=east]".formatted(x, y, z, piston));
			if (glass) server.runCommand("setblock %d %d %d minecraft:glass".formatted(x + 1, y, z));
			// The item where the front block goes when pushed, and a wall beyond.
			itemX = glass ? x + 2 : x + 1;
			itemY = y;
			server.runCommand("setblock %d %d %d minecraft:stone".formatted(itemX + 1, y, z));
			powerAt = "%d %d %d".formatted(x - 1, y, z);
		} else {
			server.runCommand("setblock %d %d %d minecraft:%s[facing=down]".formatted(x, y + 3, z, piston));
			if (glass) server.runCommand("setblock %d %d %d minecraft:glass".formatted(x, y + 2, z));
			itemX = x;
			itemY = y + (glass ? 1 : 2);
			server.runCommand("setblock %d %d %d minecraft:stone".formatted(x, y, z));
			itemY = y + 1;
			powerAt = "%d %d %d".formatted(x, y + 4, z);
		}
		context.waitTicks(4);
		if (cycle) {
			server.runCommand("setblock " + powerAt + " minecraft:redstone_block");
			context.waitTicks(6);
			server.runCommand("setblock " + powerAt + " minecraft:air");
			context.waitTicks(6);
		}
		server.runCommand("summon minecraft:item %d.5 %d %d.5 {Item:{id:\"minecraft:diamond_sword\",count:1}}".formatted(itemX, itemY, z));
		context.waitTicks(20);
		int before = count(server, itemX, itemY, z);
		server.runCommand("setblock " + powerAt + " minecraft:redstone_block");
		context.waitTicks(20);
		int after = count(server, itemX, itemY, z);
		BlockPos at = new BlockPos(itemX, itemY, z);
		int orbs = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
			new AABB(at).inflate(4)).size());
		return "  " + name + ": items " + before + " -> " + after + ", orbs " + orbs + (after > 0 || before == 0 ? "  SURVIVED" : "") + "\n";
	}

	private static int count(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(s -> s.overworld().getEntitiesOfClass(ItemEntity.class,
			new AABB(new BlockPos(x, y, z)).inflate(3)).size());
	}
}
