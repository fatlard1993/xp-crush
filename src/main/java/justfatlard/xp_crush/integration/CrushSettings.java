package justfatlard.xp_crush.integration;

import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.xp_crush.XpCrushConfig;

/**
 * The file's switches on XP Crush's page of the Pandorical mods menu, for ops. The prices and the
 * scaling stay in the file: they are a table to edit, not a switch to flip.
 *
 * <p>Only loaded when Pandorical is here: this class imports its API.
 */
public final class CrushSettings {
	private CrushSettings() {}

	public static void register() {
		var group = PandoricalApi.settings().serverGroup("xp-crush", "XP Crush");
		group.toggle("pistons", "Pistons crush items", true)
			.backedBy(player -> XpCrushConfig.pistons(), (player, on) -> XpCrushConfig.setPistons(on));
		group.toggle("fallingBlocks", "Anvils and stalactites crush items", true)
			.backedBy(player -> XpCrushConfig.fallingBlocks(), (player, on) -> XpCrushConfig.setFallingBlocks(on));
		group.toggle("fallingColumns", "Falling sand and gravel crush items", true)
			.describe("Only as a column: one block on its own pops the item out")
			.backedBy(player -> XpCrushConfig.fallingColumns(), (player, on) -> XpCrushConfig.setFallingColumns(on));
		group.number("columnMin", "Blocks in a crushing column", 2, 16, 1, 2)
			.backedBy(player -> XpCrushConfig.columnMin(), (player, blocks) -> XpCrushConfig.setColumnMin(blocks));
		// XP from mobs dying on their own lives in Pandorical now, as a rule of the world; asked on, as it was here.
		// A Pandorical from before the rules has no such call, and is left to run without it.
		try {
			justfatlard.pandorical.api.PandoricalApi.rules().unattendedKillXp(true);
		} catch (LinkageError older) {
			justfatlard.xp_crush.Main.LOGGER.warn("[{}] Pandorical is older than 15.37.0, so mobs dying on their own drop no XP", justfatlard.xp_crush.Main.MOD_ID);
		}
		group.toggle("hopperBottles", "Hoppers fill bottles with XP", true)
			.backedBy(player -> XpCrushConfig.hopperBottles(), (player, on) -> XpCrushConfig.setHopperBottles(on));
		group.number("xpPerBottle", "XP to fill a bottle", 1, 50, 1, 7)
			.backedBy(player -> XpCrushConfig.xpPerBottle(), (player, points) -> XpCrushConfig.setXpPerBottle(points));
		group.toggle("anvilBottles", "Anvils mend with XP bottles", true)
			.describe("Bottles o' enchanting repair anything with mending")
			.backedBy(player -> XpCrushConfig.anvilBottles(), (player, on) -> XpCrushConfig.setAnvilBottles(on));
		group.toggle("tomes", "XP tomes on lecterns", true)
			.backedBy(player -> XpCrushConfig.tomes(), (player, on) -> XpCrushConfig.setTomes(on));
	}
}
