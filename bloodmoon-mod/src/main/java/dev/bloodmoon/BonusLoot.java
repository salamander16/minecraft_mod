package dev.bloodmoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Extra, generous drops for mobs that were spawned or upgraded by the blood moon. */
public final class BonusLoot {
	private record Entry(int weight, Item item, int min, int max) {
	}

	private static final List<Entry> GENERIC = List.of(
		new Entry(20, Items.IRON_INGOT, 2, 5),
		new Entry(16, Items.GOLD_INGOT, 2, 5),
		new Entry(12, Items.EMERALD, 1, 4),
		new Entry(10, Items.DIAMOND, 1, 2),
		new Entry(10, Items.EXPERIENCE_BOTTLE, 3, 8),
		new Entry(7, Items.ENDER_PEARL, 1, 2),
		new Entry(6, Items.GOLDEN_APPLE, 1, 1),
		new Entry(4, Items.NETHERITE_SCRAP, 1, 1),
		new Entry(1, Items.ENCHANTED_GOLDEN_APPLE, 1, 1),
		new Entry(1, Items.TOTEM_OF_UNDYING, 1, 1),
		new Entry(1, Items.NETHERITE_INGOT, 1, 1)
	);

	private BonusLoot() {
	}

	public static void onDeath(LivingEntity entity, DamageSource source) {
		BloodMoonConfig cfg = BloodMoonConfig.get();
		if (!cfg.bonusLoot || !(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		if (!mob.getTags().contains(BloodMoonManager.TAG_BUFFED)) {
			return;
		}
		if (mob.getTags().contains(BloodMoonManager.TAG_EXTRA)) {
			BloodMoonManager.onBonusMobDied();
		}

		RandomSource random = level.getRandom();

		// Bonus experience.
		int bonusXp = (int) Math.round(Math.max(0.0, cfg.bonusXpMultiplier - 1.0) * 5.0);
		if (bonusXp > 0) {
			ExperienceOrb.award(level, mob.position(), bonusXp);
		}

		if (random.nextDouble() > cfg.bonusLootChance) {
			return;
		}

		List<ItemStack> drops = new ArrayList<>();
		int rolls = 1 + (random.nextInt(100) < 25 ? 1 : 0);
		for (int i = 0; i < rolls; i++) {
			drops.add(rollGeneric(random));
		}

		// Type-specific extras.
		if (mob instanceof Creeper) {
			drops.add(new ItemStack(Items.GUNPOWDER, 4 + random.nextInt(5)));
			if (random.nextInt(10) == 0) {
				drops.add(new ItemStack(Items.TNT, 1 + random.nextInt(2)));
			}
		} else if (mob instanceof AbstractSkeleton) {
			drops.add(new ItemStack(Items.ARROW, 8 + random.nextInt(9)));
		} else if (mob instanceof Drowned) {
			if (random.nextInt(4) == 0) {
				drops.add(new ItemStack(Items.NAUTILUS_SHELL, 1));
			}
		} else if (mob.getType() == EntityTypes.SPIDER || mob.getType() == EntityTypes.CAVE_SPIDER) {
			drops.add(new ItemStack(Items.SPIDER_EYE, 1 + random.nextInt(2)));
			drops.add(new ItemStack(Items.STRING, 2 + random.nextInt(4)));
		}

		for (ItemStack stack : drops) {
			ItemEntity itemEntity = new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), stack);
			level.addFreshEntity(itemEntity);
		}
	}

	private static ItemStack rollGeneric(RandomSource random) {
		int total = 0;
		for (Entry entry : GENERIC) {
			total += entry.weight();
		}
		int pick = random.nextInt(total);
		for (Entry entry : GENERIC) {
			pick -= entry.weight();
			if (pick < 0) {
				int count = entry.min() + (entry.max() > entry.min() ? random.nextInt(entry.max() - entry.min() + 1) : 0);
				return new ItemStack(entry.item(), count);
			}
		}
		return new ItemStack(Items.IRON_INGOT);
	}
}
