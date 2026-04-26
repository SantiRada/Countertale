package com.thescar.hygunsplugin.gameplay.projectile.ammo;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;

public interface AmmoImpactInteractionHandler {
	boolean supports(@Nonnull String type);

	void execute(@Nonnull JsonObject interaction, @Nonnull AmmoImpactInteractionContext context);
}
