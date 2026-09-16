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
		group.toggle("unattendedKills", "Mobs dying on their own drop XP", true)
			.describe("Falls, lava, golems: deaths no player caused")
			.backedBy(player -> XpCrushConfig.unattendedKills(), (player, on) -> XpCrushConfig.setUnattendedKills(on));
		group.number("unattendedKillPercent", "Of a player kill's XP, percent", 0, 100, 5, 75)
			.backedBy(player -> (int) Math.round(XpCrushConfig.unattendedKillFraction() * 100),
				(player, percent) -> XpCrushConfig.setUnattendedKillFraction(percent / 100.0));
		group.toggle("hopperBottles", "Hoppers fill bottles with XP", true)
			.backedBy(player -> XpCrushConfig.hopperBottles(), (player, on) -> XpCrushConfig.setHopperBottles(on));
		group.number("xpPerBottle", "XP to fill a bottle", 1, 50, 1, 7)
			.backedBy(player -> XpCrushConfig.xpPerBottle(), (player, points) -> XpCrushConfig.setXpPerBottle(points));
		group.toggle("tomes", "XP tomes on lecterns", true)
			.backedBy(player -> XpCrushConfig.tomes(), (player, on) -> XpCrushConfig.setTomes(on));
	}
}
