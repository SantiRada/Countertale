package com.thescar.hygunsplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class HygunsCommand extends AbstractCommandCollection {
	public HygunsCommand() {
		super("hyguns", "HyGuns utility commands");
		addAliases("hg");
		addSubCommand(new ColorRulesCommand());
		addSubCommand(new DebugCommand());
	}
}
