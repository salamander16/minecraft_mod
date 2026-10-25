package dev.bloodmoon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloodMoonMod implements ModInitializer {
	public static final String MOD_ID = "bloodmoon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BloodMoonConfig.load();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> BloodMoonManager.reset());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BloodMoonManager.reset());

		ServerTickEvents.END_SERVER_TICK.register(BloodMoonManager::tick);
		ServerTickEvents.END_LEVEL_TICK.register(ExplosiveArrows::tick);

		ServerEntityEvents.ALLOW_LOAD.register(BloodMoonManager::onAllowLoad);
		ServerEntityEvents.ENTITY_LOAD.register(BloodMoonManager::onEntityLoad);

		ServerLivingEntityEvents.AFTER_DEATH.register(BonusLoot::onDeath);

		CommandRegistrationCallback.EVENT.register(BloodMoonCommand::register);

		LOGGER.info("Blood Moon loaded - every night is a blood moon.");
	}
}
