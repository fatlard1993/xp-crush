package justfatlard.xp_crush;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * The knobs, in {@code config/xp-crush.properties}.
 *
 * <p>Two kinds. The first says what counts as crushing and how experience scales, and is small.
 * The second is the price list: what a raw material is worth in experience, for the things no
 * recipe makes. Everything crafted is priced from its ingredients, so the list only has to name
 * what comes out of the ground, out of a mob, or out of a chest - and only the ones worth more
 * than the default, which is what a lump of cobblestone gets.
 */
public final class XpCrushConfig {
	private XpCrushConfig() {}

	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("xp-crush.properties");

	private static boolean pistons = true;
	private static boolean fallingBlocks = true;
	private static boolean fallingColumns = true;
	private static int columnMin = 2;

	private static boolean unattendedKills = true;
	private static double unattendedKillFraction = 0.75;
	private static boolean hopperBottles = true;
	private static int xpPerBottle = 7;
	private static boolean tomes = true;
	private static double tomeBindingShare = 0.05;
	private static double tomeBindingSharePerThousand = 0.05;
	private static double tomeBindingShareMax = 0.5;
	private static int tomeMinimumPoints = 100;

	private static double xpPerBlockFallen = 0.1;
	private static double xpPerBlockStacked = 0.25;
	private static double xpPerSqueeze = 1.0;
	private static double forceCap = 4.0;

	private static double xpPerRawItem = 0.05;
	private static double xpPerCraftStep = 0.05;
	private static double xpPerEnchantmentLevel = 1.0;
	private static double rarityUncommon = 2.0;
	private static double rarityRare = 4.0;
	private static double rarityEpic = 8.0;

	/** Explicit prices, by item id. Anything raw and absent is worth {@link #xpPerRawItem}. */
	private static final Map<Identifier, Double> worth = new HashMap<>();

	private static final String DEFAULT_CONFIG = """
			# XP Crush configuration
			# Delete this file to regenerate with defaults.

			# What counts as crushing.
			# pistons: an item driven into a block by a moving piston block.
			# falling_blocks: an anvil or a stalactite landing on an item - anything that would
			#   hurt a mob. Has to have fallen more than one block, as it does to hurt.
			# falling_columns: a plain falling block - sand, gravel, concrete powder - landing on
			#   an item as part of a column of at least column_min blocks. One block on its own
			#   just pops the item out of the hole, so one block on its own does nothing here.
			pistons=true
			falling_blocks=true
			falling_columns=true
			column_min=2

			# Mobs that die with no player involved - the fall, the lava, the golem - drop this
			# share of the experience they would have paid a player. Deaths vanilla already pays
			# for are untouched.
			unattended_kills=true
			unattended_kill_fraction=0.75

			# A hopper with an empty glass bottle in it, or in the container it feeds, takes in
			# the experience orbs that land in it and fills the bottle once it holds this much.
			# A thrown bottle pays three to eleven, seven on average.
			hopper_bottles=true
			xp_per_bottle=7

			# Write "/xp 500" on the first line of a book and quill and set it on a lectern: five
			# hundred of your experience points go into a tome you can use later. The binding
			# spends a share of them again on top, and that share climbs with the amount: the
			# base share, plus this much more per thousand points, up to the cap. Below the
			# minimum the book is turned away, since a number that small is nearly always somebody
			# thinking in levels.
			tomes=true
			tome_binding_share=0.05
			tome_binding_share_per_thousand=0.05
			tome_binding_share_max=0.5
			tome_minimum_points=100

			# How hard the crushing was, as a multiplier on what the item pays. One for the least
			# that counts, plus this much for every block of fall beyond two, plus this much for
			# every extra block in a falling column or behind a piston head, plus this much for
			# every axis on which pistons press in from both sides at once, up to the cap.
			xp_per_block_fallen=0.1
			xp_per_block_stacked=0.25
			xp_per_squeeze=1.0
			force_cap=4.0

			# How experience scales. A crafted item is worth the sum of its ingredients, divided
			# by how many the recipe makes, plus one craft step; the cheapest recipe wins.
			xp_per_raw_item=0.05
			xp_per_craft_step=0.05
			xp_per_enchantment_level=1.0
			# Multiplied in by the item's own rarity, before enchantments.
			rarity_uncommon=2.0
			rarity_rare=4.0
			rarity_epic=8.0

			# Prices for raw materials, in experience points each. A price here is final: it is
			# used even where a recipe could make the item for less. Add a line for anything you
			# think is worth more than a lump of cobblestone.
			worth.minecraft:coal=0.1
			worth.minecraft:charcoal=0.1
			worth.minecraft:raw_copper=0.2
			worth.minecraft:copper_ingot=0.2
			worth.minecraft:raw_iron=0.5
			worth.minecraft:iron_ingot=0.5
			worth.minecraft:raw_gold=0.75
			worth.minecraft:gold_ingot=0.75
			worth.minecraft:redstone=0.1
			worth.minecraft:lapis_lazuli=0.2
			worth.minecraft:quartz=0.2
			worth.minecraft:amethyst_shard=0.2
			worth.minecraft:emerald=1.5
			worth.minecraft:diamond=2.0
			worth.minecraft:ancient_debris=6.0
			worth.minecraft:netherite_scrap=6.0
			worth.minecraft:ender_pearl=1.0
			worth.minecraft:blaze_rod=1.0
			worth.minecraft:ghast_tear=2.0
			worth.minecraft:slime_ball=0.2
			worth.minecraft:magma_cream=0.4
			worth.minecraft:leather=0.2
			worth.minecraft:phantom_membrane=1.0
			worth.minecraft:wither_skeleton_skull=8.0
			worth.minecraft:nether_star=200.0
			worth.minecraft:dragon_egg=500.0
			worth.minecraft:dragon_head=100.0
			worth.minecraft:elytra=150.0
			worth.minecraft:totem_of_undying=40.0
			worth.minecraft:shulker_shell=10.0
			worth.minecraft:heart_of_the_sea=20.0
			worth.minecraft:nautilus_shell=2.0
			worth.minecraft:trident=30.0
			worth.minecraft:enchanted_golden_apple=60.0
			worth.minecraft:sponge=3.0
			worth.minecraft:echo_shard=5.0
			worth.minecraft:saddle=5.0
			worth.minecraft:name_tag=3.0
			worth.minecraft:sniffer_egg=20.0
			worth.minecraft:netherite_upgrade_smithing_template=20.0
			worth.minecraft:music_disc_pigstep=10.0
			worth.minecraft:music_disc_otherside=10.0
			""";

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			try {
				Files.createDirectories(CONFIG_PATH.getParent());
				Files.writeString(CONFIG_PATH, DEFAULT_CONFIG);
			} catch (IOException e) {
				Main.LOGGER.error("[{}] Failed to create default config: {}", Main.MOD_ID, e.getMessage());
			}
		}

		String text;
		try {
			text = Files.readString(CONFIG_PATH);
		} catch (IOException e) {
			Main.LOGGER.error("[{}] Failed to read config, using defaults: {}", Main.MOD_ID, e.getMessage());
			text = DEFAULT_CONFIG;
		}

		Properties props = new Properties();
		try {
			props.load(new StringReader(text));
		} catch (IOException e) {
			Main.LOGGER.error("[{}] Failed to parse config, using defaults: {}", Main.MOD_ID, e.getMessage());
			text = DEFAULT_CONFIG;
		}

		pistons = getBoolean(props, "pistons", pistons);
		fallingBlocks = getBoolean(props, "falling_blocks", fallingBlocks);
		fallingColumns = getBoolean(props, "falling_columns", fallingColumns);
		columnMin = Math.max(1, getInt(props, "column_min", columnMin));
		unattendedKills = getBoolean(props, "unattended_kills", unattendedKills);
		unattendedKillFraction = Math.max(0.0, getDouble(props, "unattended_kill_fraction", unattendedKillFraction));
		hopperBottles = getBoolean(props, "hopper_bottles", hopperBottles);
		xpPerBottle = Math.max(1, getInt(props, "xp_per_bottle", xpPerBottle));
		tomes = getBoolean(props, "tomes", tomes);
		tomeBindingShare = Math.max(0.0, getDouble(props, "tome_binding_share", tomeBindingShare));
		tomeBindingSharePerThousand = Math.max(0.0, getDouble(props, "tome_binding_share_per_thousand", tomeBindingSharePerThousand));
		tomeBindingShareMax = Math.max(0.0, getDouble(props, "tome_binding_share_max", tomeBindingShareMax));
		tomeMinimumPoints = Math.max(1, getInt(props, "tome_minimum_points", tomeMinimumPoints));
		xpPerBlockFallen = Math.max(0.0, getDouble(props, "xp_per_block_fallen", xpPerBlockFallen));
		xpPerBlockStacked = Math.max(0.0, getDouble(props, "xp_per_block_stacked", xpPerBlockStacked));
		xpPerSqueeze = Math.max(0.0, getDouble(props, "xp_per_squeeze", xpPerSqueeze));
		forceCap = Math.max(1.0, getDouble(props, "force_cap", forceCap));
		xpPerRawItem = getDouble(props, "xp_per_raw_item", xpPerRawItem);
		xpPerCraftStep = getDouble(props, "xp_per_craft_step", xpPerCraftStep);
		xpPerEnchantmentLevel = getDouble(props, "xp_per_enchantment_level", xpPerEnchantmentLevel);
		rarityUncommon = getDouble(props, "rarity_uncommon", rarityUncommon);
		rarityRare = getDouble(props, "rarity_rare", rarityRare);
		rarityEpic = getDouble(props, "rarity_epic", rarityEpic);

		readPrices(text);

		Main.LOGGER.info("[{}] Config loaded from {} with {} priced items", Main.MOD_ID, CONFIG_PATH, worth.size());
	}

	/**
	 * The price list, read line by line rather than through {@link Properties}.
	 *
	 * <p>A properties file ends a key at the first colon as readily as at the first equals sign,
	 * so {@code worth.minecraft:diamond=2.0} arrives as the key {@code worth.minecraft} with the
	 * value {@code diamond=2.0}, and every line on the list collapses into one key. The list is
	 * read from the file alone, so deleting a line deletes the price rather than leaving the
	 * default underneath it.
	 */
	private static void readPrices(String text) {
		worth.clear();
		for (String raw : text.split("\\R")) {
			String line = raw.strip();
			if (!line.startsWith(PRICE_PREFIX)) continue;
			int eq = line.indexOf('=');
			if (eq < 0) {
				Main.LOGGER.warn("[{}] '{}' has no value", Main.MOD_ID, line);
				continue;
			}
			String key = line.substring(PRICE_PREFIX.length(), eq).strip();
			String value = line.substring(eq + 1).strip();
			Identifier id = Identifier.tryParse(key);
			if (id == null) {
				Main.LOGGER.warn("[{}] '{}' is not an item id", Main.MOD_ID, key);
				continue;
			}
			try {
				worth.put(id, Double.parseDouble(value));
			} catch (NumberFormatException e) {
				Main.LOGGER.warn("[{}] Invalid price for '{}': '{}', using {}", Main.MOD_ID, key, value, xpPerRawItem);
				worth.put(id, xpPerRawItem);
			}
		}
	}

	private static final String PRICE_PREFIX = "worth.";

	/**
	 * Write one value back into the file where its line stands, keeping the comments and order;
	 * added at the end when the file has no line for it.
	 */
	private static void store(String key, String value) {
		try {
			java.util.List<String> lines = Files.exists(CONFIG_PATH)
				? new java.util.ArrayList<>(Files.readAllLines(CONFIG_PATH)) : new java.util.ArrayList<>();
			boolean found = false;
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i).trim();
				if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
					lines.set(i, key + "=" + value);
					found = true;
				}
			}
			if (!found) lines.add(key + "=" + value);
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.write(CONFIG_PATH, lines);
		} catch (java.io.IOException e) {
			Main.LOGGER.error("Could not write {}: {}", CONFIG_PATH, e.getMessage());
		}
	}

	public static void setPistons(boolean on) { pistons = on; store("pistons", String.valueOf(on)); }
	public static void setFallingBlocks(boolean on) { fallingBlocks = on; store("falling_blocks", String.valueOf(on)); }
	public static void setFallingColumns(boolean on) { fallingColumns = on; store("falling_columns", String.valueOf(on)); }
	public static void setColumnMin(int blocks) { columnMin = blocks; store("column_min", String.valueOf(blocks)); }
	public static void setUnattendedKills(boolean on) { unattendedKills = on; store("unattended_kills", String.valueOf(on)); }
	public static void setUnattendedKillFraction(double fraction) { unattendedKillFraction = fraction; store("unattended_kill_fraction", String.valueOf(fraction)); }
	public static void setHopperBottles(boolean on) { hopperBottles = on; store("hopper_bottles", String.valueOf(on)); }
	public static void setXpPerBottle(int points) { xpPerBottle = points; store("xp_per_bottle", String.valueOf(points)); }
	public static void setTomes(boolean on) { tomes = on; store("tomes", String.valueOf(on)); }

	public static boolean pistons() { return pistons; }
	public static boolean fallingBlocks() { return fallingBlocks; }
	public static boolean fallingColumns() { return fallingColumns; }
	public static int columnMin() { return columnMin; }
	public static boolean unattendedKills() { return unattendedKills; }
	public static double unattendedKillFraction() { return unattendedKillFraction; }
	public static boolean hopperBottles() { return hopperBottles; }
	public static int xpPerBottle() { return xpPerBottle; }
	public static boolean tomes() { return tomes; }
	public static double tomeBindingShare() { return tomeBindingShare; }
	public static double tomeBindingSharePerThousand() { return tomeBindingSharePerThousand; }
	public static double tomeBindingShareMax() { return tomeBindingShareMax; }
	public static int tomeMinimumPoints() { return tomeMinimumPoints; }
	public static double xpPerBlockFallen() { return xpPerBlockFallen; }
	public static double xpPerBlockStacked() { return xpPerBlockStacked; }
	public static double xpPerSqueeze() { return xpPerSqueeze; }
	public static double forceCap() { return forceCap; }
	public static double xpPerRawItem() { return xpPerRawItem; }
	public static double xpPerCraftStep() { return xpPerCraftStep; }
	public static double xpPerEnchantmentLevel() { return xpPerEnchantmentLevel; }
	public static double rarityUncommon() { return rarityUncommon; }
	public static double rarityRare() { return rarityRare; }
	public static double rarityEpic() { return rarityEpic; }

	/** The configured price, or null where the item is left to the recipe tree. */
	public static Double priceOf(Identifier item) {
		return worth.get(item);
	}

	private static int getInt(Properties props, String key, int defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.isBlank()) return defaultValue;
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			Main.LOGGER.warn("[{}] Invalid value for '{}': '{}', using {}", Main.MOD_ID, key, value, defaultValue);
			return defaultValue;
		}
	}

	private static double getDouble(Properties props, String key, double defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.isBlank()) return defaultValue;
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			Main.LOGGER.warn("[{}] Invalid value for '{}': '{}', using {}", Main.MOD_ID, key, value, defaultValue);
			return defaultValue;
		}
	}

	private static boolean getBoolean(Properties props, String key, boolean defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.isBlank()) return defaultValue;
		String trimmed = value.trim();
		if (trimmed.equalsIgnoreCase("true")) return true;
		if (trimmed.equalsIgnoreCase("false")) return false;
		Main.LOGGER.warn("[{}] Invalid value for '{}': '{}', using {}", Main.MOD_ID, key, value, defaultValue);
		return defaultValue;
	}
}
