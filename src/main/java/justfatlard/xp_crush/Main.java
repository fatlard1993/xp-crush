package justfatlard.xp_crush;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crush a dropped item and get experience back.
 *
 * <p>An item on the floor is worth what it took to make, and vanilla has one way to cash that in:
 * nothing. It burns, it despawns, it falls in lava. This gives the same materials a second life
 * as experience, on the condition that something actually crushes them: a piston driving them
 * into a wall, an anvil landing on them, or a column of sand burying them. The amount is worked
 * out from the recipe tree, so a diamond pickaxe pays what its diamonds pay and a stack of
 * cobblestone pays for being a stack of cobblestone.
 *
 * <p>And a few more ways for experience to move: a mob that died with nobody's help pays a share,
 * a hopper with an empty bottle fills it from the orbs that land in it, and a lectern binds your
 * levels into a book you can carry.
 *
 * <p>Server-side only. A vanilla client sees items vanish and orbs appear, which is all there is
 * to see.
 */
public class Main implements ModInitializer {
	public static final String MOD_ID = "xp-crush";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		XpCrushConfig.load();
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("pandorical")) {
			justfatlard.xp_crush.integration.CrushSettings.register();
		}

		// The recipe tree is read once the recipes exist, and again whenever a reload changes them.
		ServerLifecycleEvents.SERVER_STARTED.register(Worth::rebuild);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) Worth.rebuild(server);
		});

		UseBlockCallback.EVENT.register(Tome::onUseBlock);
		UseItemCallback.EVENT.register(Tome::onUseItem);

		LOGGER.info("XP Crush loaded");
	}
}
