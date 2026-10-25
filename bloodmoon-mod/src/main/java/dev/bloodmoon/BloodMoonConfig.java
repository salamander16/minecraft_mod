package dev.bloodmoon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings, stored in {@code config/bloodmoon.json}. The file is created on first start
 * and can be reloaded in-game with {@code /bloodmoon reload}.
 */
public final class BloodMoonConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static BloodMoonConfig instance = new BloodMoonConfig();

	// ---- Schedule ---------------------------------------------------------------------------
	/** Legacy setting: blood moon gameplay now runs every night. */
	public int intervalNights = 1;
	/** Day-time tick (0-23999) at which "night" starts / ends for the blood moon. */
	public int nightStartTick = 13000;
	public int nightEndTick = 23000;

	// ---- Atmosphere -------------------------------------------------------------------------
	public boolean announce = true;
	/** Thunderstorm during the blood moon (also makes Channeling tridents summon lightning). */
	public boolean thunderstorm = true;

	// ---- Spawns & stats ---------------------------------------------------------------------
	/** 2.0 = double. 3.0 = triple. Fractions are rolled as chances. */
	public double spawnMultiplier = 2.0;
	/** Hard cap on bonus mobs alive at the same time (protects server TPS). */
	public int maxExtraMobs = 150;
	public double healthMultiplier = 1.5;
	public double followRange = 48.0;

	// ---- Mob specifics ----------------------------------------------------------------------
	public int creeperSpeedAmplifier = 1; // 0 = Speed I, 1 = Speed II
	public boolean invisibleSpiders = true;
	public boolean zombieArmor = true;
	public double zombieWeaponChance = 0.25;
	public double explosiveSkeletonChance = 0.05;
	public float explosiveArrowPower = 2.0F;
	/** Explosions follow the mobGriefing gamerule when true; never break blocks when false. */
	public boolean explosionsBreakBlocks = true;
	public double drownedTridentChance = 1.0;

	// ---- Zombies breaking into your base ----------------------------------------------------
	public boolean breakEntrances = true;
	public boolean breakDoors = true;
	public boolean breakIronDoors = false;
	public boolean breakFenceGates = true;
	public boolean breakFences = true;
	public boolean breakTrapdoors = true;
	/** Ticks of "hitting" one block before it breaks (20 ticks = 1 second). */
	public int breakTicks = 100;
	/** How long a zombie must be stuck before it starts breaking something. */
	public int stuckTicks = 30;

	// ---- Loot -------------------------------------------------------------------------------
	public boolean bonusLoot = true;
	public double bonusLootChance = 0.6;
	public double bonusXpMultiplier = 2.0;

	public static BloodMoonConfig get() {
		return instance;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("bloodmoon.json");
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					BloodMoonConfig loaded = GSON.fromJson(reader, BloodMoonConfig.class);
					if (loaded != null) {
						instance = loaded;
					}
				}
			}
			instance.sanitize();
			// Always write back so new options added by updates show up in the file.
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (Exception e) {
			BloodMoonMod.LOGGER.error("Could not read/write {} - using defaults/previous values", path, e);
		}
	}

	private void sanitize() {
		intervalNights = Math.max(1, intervalNights);
		nightStartTick = Math.floorMod(nightStartTick, 24000);
		nightEndTick = Math.max(nightStartTick + 1, Math.min(24000, nightEndTick));
		spawnMultiplier = Math.max(1.0, Math.min(8.0, spawnMultiplier));
		maxExtraMobs = Math.max(0, maxExtraMobs);
		healthMultiplier = Math.max(0.1, healthMultiplier);
		breakTicks = Math.max(10, breakTicks);
		stuckTicks = Math.max(5, stuckTicks);
	}
}
