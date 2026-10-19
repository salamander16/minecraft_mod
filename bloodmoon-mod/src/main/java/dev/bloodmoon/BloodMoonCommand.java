package dev.bloodmoon;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/** /bloodmoon [status|start|stop|reload] */
public final class BloodMoonCommand {
	private BloodMoonCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
		dispatcher.register(Commands.literal("bloodmoon")
			.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.executes(BloodMoonCommand::status)
			.then(Commands.literal("status").executes(BloodMoonCommand::status))
			.then(Commands.literal("start").executes(context -> {
				BloodMoonManager.forceStart(context.getSource().getServer());
				context.getSource().sendSystemMessage(Component.literal("Blood Moon started (lasts until dawn)."));
				return Command.SINGLE_SUCCESS;
			}))
			.then(Commands.literal("stop").executes(context -> {
				BloodMoonManager.forceStop(context.getSource().getServer());
				context.getSource().sendSystemMessage(Component.literal("Blood Moon stopped for tonight."));
				return Command.SINGLE_SUCCESS;
			}))
			.then(Commands.literal("reload").executes(context -> {
				BloodMoonConfig.load();
				context.getSource().sendSystemMessage(Component.literal("Blood Moon config reloaded."));
				return Command.SINGLE_SUCCESS;
			}))
		);
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSystemMessage(Component.literal(BloodMoonManager.describe(context.getSource().getServer())));
		return Command.SINGLE_SUCCESS;
	}
}
