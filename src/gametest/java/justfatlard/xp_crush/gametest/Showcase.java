package justfatlard.xp_crush.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;

/**
 * The pictures for the readme and the mod page: an anvil coming down on a pile of gear, and the
 * experience that comes back off it.
 *
 * <p>The crush is the mod's own: a falling anvil landing on dropped items, which is one of the
 * three things that crush. Run it under xvfb-run with :runClientGameTest - the bare task name runs
 * in every included project too - and the frames land in build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {

	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();

			context.getInput().pressKey(options -> options.keyToggleGui);
			// Orbs chase a player within eight blocks, so the camera has to stand off or they end
			// up sitting on the lens. A narrow field of view brings them back without moving in.
			context.runOnClient(client -> client.options.fov().set(30));
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doWeatherCycle false");
			server.runCommand("gamerule doTileDrops true");
			server.runCommand("weather clear");
			server.runCommand("time set 1000");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			int floor = z - 6;

			// A stone floor to land on, and a pile of gear worth something lying on it.
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone"
				.formatted(x - 6, y - 1, floor - 6, x + 6, y - 1, floor + 4));
			// A haul rather than a trinket: the orbs merge as they spread, so a small payout comes
			// out as one speck no matter when the shutter falls.
			for (String item : new String[] {"diamond_pickaxe", "diamond_chestplate", "golden_apple",
					"diamond_sword", "diamond_axe", "netherite_ingot", "diamond_helmet",
					"enchanted_golden_apple", "diamond_boots", "diamond_leggings"}) {
				server.runCommand("summon minecraft:item %d %d %d {Item:{id:\"minecraft:%s\",count:1}}"
					.formatted(x, y, floor, item));
			}
			// An anvil from high enough to pay well: each block of fall past the second adds.
			server.runCommand("setblock %d %d %d minecraft:anvil".formatted(x, y + 12, floor));
			server.runCommand("setblock %d %d %d minecraft:air".formatted(x, y + 11, floor));

			context.waitTicks(10);
			String before = server.computeOnServer(s -> "items=" + s.overworld().getEntitiesOfClass(
				net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(new BlockPos(x, y, floor)).inflate(4)).size());

			look(server, x + 5.0, y + 0.0, floor + 6.5, x, y + 0.6, floor);
			// The moment it lands, not a moment later: an anvil from twelve up takes about
			// twenty-six ticks, and the orbs are at their thickest right after that before they
			// spread out and start merging.
			context.waitTicks(29);
			shoot(context, "crushed");
			int orbs = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(
				net.minecraft.world.entity.ExperienceOrb.class,
				new net.minecraft.world.phys.AABB(new BlockPos(x, y, floor)).inflate(8)).size());
			if (orbs == 0) throw new AssertionError("nothing was crushed: " + before + " orbs=0");
		}
	}

	/** Stand the camera at one place and point it at another; the y is the feet. */
	private void look(TestServerContext server, double x, double y, double z,
			double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}

	private void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}
}
