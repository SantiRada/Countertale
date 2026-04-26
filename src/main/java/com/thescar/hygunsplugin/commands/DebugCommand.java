package com.thescar.hygunsplugin.commands;

import com.thescar.hygunsplugin.debug.DebugSettings;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class DebugCommand extends CommandBase {
	private final RequiredArg<String> stateArg;

	public DebugCommand() {
		super("debug", "Enable or disable global HyGuns debug overlay");
		this.stateArg = withRequiredArg("state", "Expected boolean-like value", ArgTypes.STRING);
	}

	@Override
	protected void executeSync(CommandContext context) {
		String input = this.stateArg.get(context);
		if (input == null) {
			context.sendMessage(Message.raw("Usage: /hyguns debug <on|off>"));
			return;
		}

		String normalized = input.trim();
		if (!ValueUtils.Boolean.isKnown(normalized)) {
			context.sendMessage(Message.raw("Invalid debug state. Use a boolean-like value such as on/off, true/false, yes/no, 1/0."));
			return;
		}

		boolean enabled = ValueUtils.Boolean.parse(normalized);
		DebugSettings.setEnabled(enabled);
		context.sendMessage(Message.join(
			Message.raw("HyGuns debug is now "), Message
				.raw(enabled
				     ? "Enabled"
				     : "Disabled")
				.color(enabled
				       ? "#00ff00"
				       : "#ff0000")
				.bold(true)
		));
	}
}
