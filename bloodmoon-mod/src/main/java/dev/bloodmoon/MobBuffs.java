package dev.bloodmoon;

import dev.bloodmoon.mixin.MobAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Applies the blood-moon upgrades to a freshly spawned monster. */
public final class MobBuffs {
	private static final int INFINITE = -1;

	private MobBuffs() {
	}

	public static void apply(Mob mob, ServerLevel level) {
		if (mob.getTags().contains(BloodMoonManager.TAG_BUFFED)) {
			return;
		}
		mob.addTag(BloodMoonManager.TAG_BUFFED);

		BloodMoonConfig cfg = BloodMoonConfig.get();
		RandomSource random = level.getRandom();

		// --- All monsters: tougher and they notice you from further away ---
		scale(mob, Attributes.MAX_HEALTH, cfg.healthMultiplier);
		mob.setHealth(mob.getMaxHealth());
		raiseTo(mob, Attributes.FOLLOW_RANGE, cfg.followRange);

		// --- Creepers: fast ---
		if (mob instanceof Creeper) {
			mob.addEffect(new MobEffectInstance(MobEffects.SPEED, INFINITE, cfg.creeperSpeedAmplifier, false, false));
		}

		// --- Spiders (and cave spiders): invisible ---
		if (cfg.invisibleSpiders && (mob.getType() == EntityTypes.SPIDER || mob.getType() == EntityTypes.CAVE_SPIDER)) {
			mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INFINITE, 0, false, false));
		}

		// --- Skeletons (skeleton, stray, bogged ...): a few carry explosive bows ---
		if (mob instanceof AbstractSkeleton skeleton
			&& skeleton.getMainHandItem().is(Items.BOW)
			&& random.nextDouble() < cfg.explosiveSkeletonChance) {
			skeleton.addTag(BloodMoonManager.TAG_EXPLOSIVE);
			ItemStack bow = new ItemStack(Items.BOW);
			bow.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // glowing bow = warning sign
			enchant(level, bow, Enchantments.POWER, 5);
			skeleton.setItemSlot(EquipmentSlot.MAINHAND, bow);
		}

		// --- Zombies (zombie, husk, drowned, villager zombie): armour + they break entrances ---
		if (mob instanceof Zombie zombie) {
			if (cfg.zombieArmor) {
				armorUp(level, zombie, random);
			}
			if (cfg.breakEntrances) {
				((MobAccessor) (Object) zombie).bloodmoon$getGoalSelector().addGoal(1, new BreachGoal(zombie));
			}
		}

		// --- Drowned: channeling tridents ---
		if (mob instanceof Drowned drowned && random.nextDouble() < cfg.drownedTridentChance) {
			ItemStack trident = drowned.getMainHandItem().is(Items.TRIDENT) ? drowned.getMainHandItem() : new ItemStack(Items.TRIDENT);
			enchant(level, trident, Enchantments.CHANNELING, 1);
			enchant(level, trident, Enchantments.IMPALING, 3);
			drowned.setItemSlot(EquipmentSlot.MAINHAND, trident);
		}
	}

	private static void armorUp(ServerLevel level, Zombie zombie, RandomSource random) {
		boolean netherite = random.nextBoolean();
		equip(level, zombie, EquipmentSlot.HEAD, netherite ? Items.NETHERITE_HELMET : Items.DIAMOND_HELMET, random);
		equip(level, zombie, EquipmentSlot.CHEST, netherite ? Items.NETHERITE_CHESTPLATE : Items.DIAMOND_CHESTPLATE, random);
		equip(level, zombie, EquipmentSlot.LEGS, netherite ? Items.NETHERITE_LEGGINGS : Items.DIAMOND_LEGGINGS, random);
		equip(level, zombie, EquipmentSlot.FEET, netherite ? Items.NETHERITE_BOOTS : Items.DIAMOND_BOOTS, random);

		if (!(zombie instanceof Drowned) && random.nextDouble() < BloodMoonConfig.get().zombieWeaponChance) {
			ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
			enchant(level, sword, Enchantments.SHARPNESS, 5);
			zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
		}
	}

	private static void equip(ServerLevel level, Mob mob, EquipmentSlot slot, Item item, RandomSource random) {
		ItemStack stack = new ItemStack(item);
		enchant(level, stack, Enchantments.PROTECTION, 4);
		enchant(level, stack, Enchantments.UNBREAKING, 3);
		if (random.nextDouble() < 0.5) {
			enchant(level, stack, Enchantments.THORNS, 2);
		}
		mob.setItemSlot(slot, stack);
	}

	public static void enchant(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key, int enchantLevel) {
		try {
			Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
			EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, enchantLevel));
		} catch (Exception e) {
			BloodMoonMod.LOGGER.warn("Could not apply enchantment {}", key, e);
		}
	}

	private static void scale(Mob mob, Holder<Attribute> attribute, double factor) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(instance.getBaseValue() * factor);
		}
	}

	private static void raiseTo(Mob mob, Holder<Attribute> attribute, double minimum) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null && instance.getBaseValue() < minimum) {
			instance.setBaseValue(minimum);
		}
	}
}
