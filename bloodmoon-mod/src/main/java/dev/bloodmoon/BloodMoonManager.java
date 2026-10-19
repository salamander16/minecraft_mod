package dev.bloodmoon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Decides when the blood moon is active and handles its start, end and extra spawns. */
public final class BloodMoonManager {
	/** Mob already received blood-moon buffs. */
	public static final String TAG_BUFFED = "bloodmoon_buffed";
	/** Mob is a bonus (duplicate) mob and is removed at dawn. */
	public static final String TAG_EXTRA = "bloodmoon_extra";
	/** Skeleton shoots explosive arrows. */
	public static final String TAG_EXPLOSIVE = "bloodmoon_explosive";

	private static boolean active = false;
	private static boolean forced = false;
	private static boolean wasNight = false;
	private static long suppressedDay = Long.MIN_VALUE;
	private static int tickCounter = 0;
	private static int extraAlive = 0;
	private static final List<Mob> PENDING_DUPLICATES = new ArrayList<>();

	private BloodMoonManager() {
	}

	public static boolean isActive() {
		return active;
	}

	public static void reset() {
		active = false;
		forced = false;
		wasNight = false;
		suppressedDay = Long.MIN_VALUE;
		tickCounter = 0;
		extraAlive = 0;
		PENDING_DUPLICATES.clear();
		ExplosiveArrows.clear();
	}

	// ------------------------------------------------------------------------------------------
	// Schedule
	// ------------------------------------------------------------------------------------------

	public static void tick(MinecraftServer server) {
		if (!PENDING_DUPLICATES.isEmpty()) {
			processDuplicates();
		}
		if (++tickCounter % 10 != 0) {
			return;
		}
		evaluate(server);
	}

	/** Re-checks the clock and starts/ends the blood moon if needed. */
	public static void evaluate(MinecraftServer server) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		ServerLevel overworld = server.overworld();
		long clock = overworld.getDefaultClockTime();
		long day = Math.floorDiv(clock, 24000L);
		long timeOfDay = Math.floorMod(clock, 24000L);

		boolean night = timeOfDay >= cfg.nightStartTick && timeOfDay < cfg.nightEndTick;
		if (wasNight && !night) {
			forced = false; // a forced blood moon ends at dawn
		}
		wasNight = night;

		boolean scheduled = night && (day + 1) % cfg.intervalNights == 0 && day != suppressedDay;
		boolean shouldBeActive = scheduled || forced;

		if (shouldBeActive && !active) {
			start(server, timeOfDay);
		} else if (!shouldBeActive && active) {
			end(server, overworld);
		}
	}

	public static void forceStart(MinecraftServer server) {
		forced = true;
		suppressedDay = Long.MIN_VALUE;
		evaluate(server);
	}

	public static void forceStop(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		forced = false;
		suppressedDay = Math.floorDiv(overworld.getDefaultClockTime(), 24000L);
		evaluate(server);
	}

	public static String describe(MinecraftServer server) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		long clock = server.overworld().getDefaultClockTime();
		long day = Math.floorDiv(clock, 24000L);
		long timeOfDay = Math.floorMod(clock, 24000L);
		if (active) {
			return "The Blood Moon is rising right now! (night " + (day + 1) + ")";
		}
		long nightNo = day + 1 + (timeOfDay >= cfg.nightEndTick ? 1 : 0);
		long next = ((nightNo + cfg.intervalNights - 1) / cfg.intervalNights) * cfg.intervalNights;
		long left = next - nightNo;
		return "Next Blood Moon: night " + next + (left == 0 ? " (tonight!)" : " (in " + left + " night" + (left == 1 ? "" : "s") + ")");
	}

	private static void start(MinecraftServer server, long timeOfDay) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		active = true;
		extraAlive = 0;
		PENDING_DUPLICATES.clear();
		BloodMoonMod.LOGGER.info("The Blood Moon rises.");

		if (cfg.announce) {
			run(server, "title @a times 20 100 40");
			run(server, "title @a subtitle {\"text\":\"The night turns red. Guard your doors.\",\"color\":\"red\"}");
			run(server, "title @a title {\"text\":\"Blood Moon\",\"color\":\"dark_red\",\"bold\":true}");
			run(server, "playsound minecraft:entity.wither.spawn master @a");
		}
		if (cfg.thunderstorm) {
			int duration = Math.max(1200, cfg.nightEndTick - (int) timeOfDay);
			run(server, "weather thunder " + duration);
		}
	}

	private static void end(MinecraftServer server, ServerLevel overworld) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		active = false;
		PENDING_DUPLICATES.clear();
		BloodMoonMod.LOGGER.info("The Blood Moon fades.");

		// Remove bonus mobs.
		List<Entity> extras = new ArrayList<>();
		for (Entity entity : overworld.getAllEntities()) {
			if (entity.getTags().contains(TAG_EXTRA)) {
				extras.add(entity);
			}
		}
		for (Entity entity : extras) {
			entity.discard();
		}
		extraAlive = 0;

		if (cfg.announce) {
			run(server, "title @a actionbar {\"text\":\"The Blood Moon fades with the dawn...\",\"color\":\"gray\"}");
		}
		if (cfg.thunderstorm) {
			run(server, "weather clear");
		}
	}

	private static void run(MinecraftServer server, String command) {
		try {
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
		} catch (Exception e) {
			BloodMoonMod.LOGGER.warn("Command failed: {}", command, e);
		}
	}

	// ------------------------------------------------------------------------------------------
	// Entity events
	// ------------------------------------------------------------------------------------------

	/** Fabric ALLOW_LOAD: lets us see the spawn reason. We never block normal spawns. */
	public static boolean onAllowLoad(Entity entity, ServerLevel level, EntitySpawnReason reason, boolean loadedFromDisk) {
		if (loadedFromDisk) {
			// Bonus mobs that were saved to disk and are loaded after the blood moon ended: delete them.
			return active || !entity.getTags().contains(TAG_EXTRA);
		}
		if (!active || reason != EntitySpawnReason.NATURAL) {
			return true;
		}
		if (level.dimension() != Level.OVERWORLD || !isBloodMoonTarget(entity)) {
			return true;
		}
		Mob mob = (Mob) entity;
		if (mob.getTags().contains(TAG_BUFFED)) {
			return true;
		}

		MobBuffs.apply(mob, level);

		BloodMoonConfig cfg = BloodMoonConfig.get();
		if (cfg.spawnMultiplier > 1.0 && mob.getY() >= level.getSeaLevel() - 16) {
			PENDING_DUPLICATES.add(mob);
		}
		return true;
	}

	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof AbstractArrow arrow) {
			ExplosiveArrows.maybeTrack(arrow);
		}
	}

	public static void onBonusMobDied() {
		extraAlive = Math.max(0, extraAlive - 1);
	}

	private static boolean isBloodMoonTarget(Entity entity) {
		if (!(entity instanceof Mob)) {
			return false;
		}
		if (entity.getType().getCategory() != MobCategory.MONSTER) {
			return false;
		}
		return !(entity instanceof WitherBoss || entity instanceof EnderDragon || entity instanceof Warden);
	}

	// ------------------------------------------------------------------------------------------
	// Doubling
	// ------------------------------------------------------------------------------------------

	private static void processDuplicates() {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		List<Mob> work = new ArrayList<>(PENDING_DUPLICATES);
		PENDING_DUPLICATES.clear();

		for (Mob original : work) {
			if (!active || original.isRemoved() || !(original.level() instanceof ServerLevel level)) {
				continue;
			}
			double extra = cfg.spawnMultiplier - 1.0;
			int copies = (int) Math.floor(extra);
			if (level.getRandom().nextDouble() < extra - copies) {
				copies++;
			}
			for (int i = 0; i < copies; i++) {
				if (extraAlive >= cfg.maxExtraMobs) {
					return;
				}
				if (spawnCopy(level, original)) {
					extraAlive++;
				}
			}
		}
	}

	private static boolean spawnCopy(ServerLevel level, Mob original) {
		Entity raw = original.getType().create(level, EntitySpawnReason.EVENT);
		if (!(raw instanceof Mob copy)) {
			return false;
		}

		RandomSource random = level.getRandom();
		boolean placed = false;
		for (int attempt = 0; attempt < 8 && !placed; attempt++) {
			copy.setPos(
				original.getX() + (random.nextDouble() - 0.5) * 6.0,
				original.getY(),
				original.getZ() + (random.nextDouble() - 0.5) * 6.0
			);
			placed = level.noCollision(copy.getBoundingBox());
		}
		if (!placed) {
			copy.setPos(original.getX(), original.getY(), original.getZ());
		}
		copy.setYRot(random.nextFloat() * 360.0F);

		copy.finalizeSpawn(level, level.getCurrentDifficultyAt(copy.blockPosition()), EntitySpawnReason.EVENT, null);
		copy.addTag(TAG_EXTRA);
		// Persistent mobs do not count against the vanilla mob cap, so natural spawns keep happening.
		// They are removed by us at dawn.
		copy.setPersistenceRequired();
		MobBuffs.apply(copy, level);
		return level.addFreshEntity(copy);
	}
}
