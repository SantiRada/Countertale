package com.thescar.hygunsplugin.commands;

import com.thescar.hygunsplugin.content.particles.ParticleColorRuleRegistry;
import com.thescar.hygunsplugin.content.particles.ParticleColorVariantService;
import com.thescar.hygunsplugin.content.particles.ParticleInteractionPaletteRegistry;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ColorRulesCommand extends CommandBase {
	public ColorRulesCommand() {
		super("colorrules", "Reload HyGuns particle color rules");
		addAliases("color-rules", "cr");
	}

	@Override
	protected void executeSync(CommandContext context) {
		ParticleColorRuleRegistry.refresh();
		ParticleInteractionPaletteRegistry.refresh();
		rememberCommandSender(context);
		ParticleColorVariantService.clearCaches();
		int handlers = ParticleColorVariantService.resendToKnownHandlers();
		context.sendMessage(Message.raw(
			"HyGuns color rules reloaded: " + ParticleColorRuleRegistry.size()
				+ " rule set(s): " + String.join(", ", ParticleColorRuleRegistry.spawnerIds())
				+ ". Resent to " + handlers + " client(s). "
				+ ParticleColorVariantService.debugState()
		));
	}

	private static void rememberCommandSender(CommandContext context) {
		if (context == null || !context.isPlayer()) {
			return;
		}
		try {
			Ref<EntityStore> ref = context.senderAsPlayerRef();
			if (ref == null || ref.getStore() == null) {
				return;
			}
			PlayerRef playerRef = PlayerRefAccess.getValid(ref, ref.getStore());
			ParticleColorVariantService.rememberPlayer(playerRef);
		} catch (Exception ignored) {
			// Console and unusual command senders do not always expose a player ref.
		}
	}
}
