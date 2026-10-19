package dev.bloodmoon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Makes a zombie smash through "entrances" (doors, fence gates, fences, trapdoors) when it has a
 * target but is stuck behind one. Only active during the blood moon.
 */
public class BreachGoal extends Goal {
	private final Mob mob;
	private BlockPos target;
	private int progress;
	private int lastTick;
	private int lastSoundTick;
	private int stillSince;
	private Vec3 lastPos = Vec3.ZERO;

	public BreachGoal(Mob mob) {
		this.mob = mob;
	}

	@Override
	public boolean canUse() {
		if (!BloodMoonManager.isActive()) {
			return false;
		}
		LivingEntity victim = mob.getTarget();
		if (victim == null || !victim.isAlive() || mob.distanceToSqr(victim) > 24.0 * 24.0) {
			return false;
		}

		Vec3 pos = mob.position();
		if (pos.distanceToSqr(lastPos) > 0.04) {
			lastPos = pos;
			stillSince = mob.tickCount;
			return false;
		}
		if (mob.tickCount - stillSince < BloodMoonConfig.get().stuckTicks) {
			return false;
		}

		BlockPos blocker = findBlocker(victim);
		if (blocker == null) {
			return false;
		}
		this.target = blocker;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (target == null || !BloodMoonManager.isActive() || mob.getTarget() == null) {
			return false;
		}
		Level level = mob.level();
		return isBreakable(level.getBlockState(target)) && mob.distanceToSqr(Vec3.atCenterOf(target)) < 6.25;
	}

	@Override
	public void start() {
		progress = 0;
		lastTick = mob.tickCount;
		lastSoundTick = mob.tickCount;
	}

	@Override
	public void tick() {
		if (target == null || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		BloodMoonConfig cfg = BloodMoonConfig.get();

		int now = mob.tickCount;
		progress += Math.max(1, now - lastTick);
		lastTick = now;

		level.destroyBlockProgress(mob.getId(), target, Math.min(9, progress * 10 / cfg.breakTicks));

		if (now - lastSoundTick >= 15) {
			lastSoundTick = now;
			level.levelEvent(1019, target, 0); // zombie bangs on wooden door
		}

		if (progress >= cfg.breakTicks) {
			level.destroyBlockProgress(mob.getId(), target, -1);
			level.destroyBlock(target, false);
			target = null;
			progress = 0;
			stillSince = now;
		}
	}

	@Override
	public void stop() {
		if (target != null && mob.level() instanceof ServerLevel level) {
			level.destroyBlockProgress(mob.getId(), target, -1);
		}
		target = null;
		progress = 0;
	}

	/** Looks one block in front of the zombie (feet and head height) towards its victim. */
	private BlockPos findBlocker(LivingEntity victim) {
		double dx = victim.getX() - mob.getX();
		double dz = victim.getZ() - mob.getZ();
		int sx = (int) Math.signum(dx);
		int sz = (int) Math.signum(dz);
		int[][] directions = Math.abs(dx) >= Math.abs(dz)
			? new int[][] {{sx, 0}, {0, sz}}
			: new int[][] {{0, sz}, {sx, 0}};

		Level level = mob.level();
		BlockPos base = mob.blockPosition();
		for (int[] direction : directions) {
			if (direction[0] == 0 && direction[1] == 0) {
				continue;
			}
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos pos = base.offset(direction[0], dy, direction[1]);
				if (isBreakable(level.getBlockState(pos))) {
					return pos;
				}
			}
		}
		return null;
	}

	private static boolean isBreakable(BlockState state) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		return (cfg.breakDoors && (state.is(BlockTags.WOODEN_DOORS) || (cfg.breakIronDoors && state.is(BlockTags.DOORS))))
			|| (cfg.breakFenceGates && state.is(BlockTags.FENCE_GATES))
			|| (cfg.breakFences && state.is(BlockTags.WOODEN_FENCES))
			|| (cfg.breakTrapdoors && state.is(BlockTags.WOODEN_TRAPDOORS));
	}
}
