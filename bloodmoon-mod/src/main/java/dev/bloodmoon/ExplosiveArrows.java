package dev.bloodmoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Arrows fired by "explosive" skeletons are tracked and detonate when they hit something
 * (an entity, which removes the arrow, or a block, where the arrow stops moving).
 */
public final class ExplosiveArrows {
	private static final class Tracked {
		final AbstractArrow arrow;
		Vec3 last;

		Tracked(AbstractArrow arrow) {
			this.arrow = arrow;
			this.last = arrow.position();
		}
	}

	private static final List<Tracked> TRACKED = new ArrayList<>();

	private ExplosiveArrows() {
	}

	public static void clear() {
		TRACKED.clear();
	}

	public static void maybeTrack(AbstractArrow arrow) {
		Entity owner = arrow.getOwner();
		if (owner != null && owner.getTags().contains(BloodMoonManager.TAG_EXPLOSIVE)) {
			TRACKED.add(new Tracked(arrow));
		}
	}

	public static void tick(ServerLevel level) {
		if (TRACKED.isEmpty()) {
			return;
		}
		Iterator<Tracked> iterator = TRACKED.iterator();
		while (iterator.hasNext()) {
			Tracked tracked = iterator.next();
			AbstractArrow arrow = tracked.arrow;
			if (arrow.level() != level) {
				continue;
			}

			Entity.RemovalReason reason = arrow.getRemovalReason();
			if (reason != null) {
				// Arrow was removed (it hit an entity, or was killed). Chunk unloads do not explode.
				iterator.remove();
				if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
					explode(level, arrow, tracked.last);
				}
				continue;
			}

			Vec3 now = arrow.position();
			boolean stuckInBlock = arrow.tickCount > 2 && now.distanceToSqr(tracked.last) < 1.0E-4;
			if (stuckInBlock || arrow.tickCount > 400) {
				iterator.remove();
				explode(level, arrow, now);
				arrow.discard();
				continue;
			}
			tracked.last = now;
		}
	}

	private static void explode(ServerLevel level, AbstractArrow arrow, Vec3 pos) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		Level.ExplosionInteraction mode = cfg.explosionsBreakBlocks
			? Level.ExplosionInteraction.MOB
			: Level.ExplosionInteraction.NONE;
		level.explode(arrow.getOwner(), pos.x, pos.y, pos.z, cfg.explosiveArrowPower, false, mode);
	}
}
