package com.thescar.hygunsplugin.ui.hud.screens;

import com.thescar.hygunsplugin.support.text.StringUtil;
import com.thescar.hygunsplugin.ui.hud.core.HudScreenContract;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nullable;

public final class AmmoScreenContract implements HudScreenContract<AmmoScreenContract.State> {
	public static final String SCREEN_ID = "hyguns.ammo";
	private static final String AMMO_UI_PATH = "Hud/HygunsAmmo.ui";
	private static final String AMMO_SELECTOR = "#AmmoValue.TextSpans";
	private static final String MAX_AMMO_SELECTOR = "#MaxAmmoValue.TextSpans";
	private static final String AMMO_ROOT_VISIBLE_SELECTOR = "#AmmoRoot.Visible";
	private static final String AMMO_ICON_VISIBLE_SELECTOR = "#AmmoIcon.Visible";
	private static final String AMMO_ICON_BG_SELECTOR = "#AmmoIcon.Background";
	private static final String WEAPON_ICON_VISIBLE_SELECTOR = "#WeaponIcon.Visible";
	private static final String WEAPON_ICON_BG_SELECTOR = "#WeaponIcon.Background";

	private static final State HIDDEN = new State("", 0, 0, 0, false, null, null);

	private static void apply(UICommandBuilder b, State state, boolean visible) {
		b.set(AMMO_ROOT_VISIBLE_SELECTOR, visible);
		if (!visible) {
			b.set(AMMO_ICON_VISIBLE_SELECTOR, false);
			b.set(AMMO_ICON_BG_SELECTOR, "(TexturePath: \"\")");
			b.set(WEAPON_ICON_VISIBLE_SELECTOR, false);
			b.set(WEAPON_ICON_BG_SELECTOR, "(TexturePath: \"\")");
			b.set(AMMO_SELECTOR, Message.raw(""));
			b.set(MAX_AMMO_SELECTOR, Message.raw(""));
			return;
		}

		syncAmmoIcon(b, state.ammoIcon);
		syncWeaponIcon(b, state.weaponIcon);
		b.set(AMMO_SELECTOR, ammoValueText(state.ammo, state.maxAmmo, state.reloading));
		b.set(MAX_AMMO_SELECTOR, maxAmmoValueText(state.reserveAmmo));
	}

	private static Message ammoValueText(int ammo, int maxAmmo, boolean reloading) {
		if (reloading) {
			return Message.raw("...");
		}

		int maxLength = String.valueOf(Math.max(0, maxAmmo)).length();
		if (maxLength < 3) {
			maxLength = 3;
		}

		return Message.raw(StringUtil.leftPad(String.valueOf(Math.max(0, ammo)), maxLength, '0'));
	}

	private static Message maxAmmoValueText(int reserveAmmo) {
		int maxLength = String.valueOf(Math.max(0, reserveAmmo)).length();
		if (maxLength < 4) {
			maxLength = 4;
		}

		return Message.raw(StringUtil.leftPad(String.valueOf(Math.max(0, reserveAmmo)), maxLength, '0'));
	}

	private static void syncAmmoIcon(UICommandBuilder b, @Nullable String ammoIcon) {
		if (ammoIcon == null || ammoIcon.isBlank()) {
			b.set(AMMO_ICON_VISIBLE_SELECTOR, false);
			b.set(AMMO_ICON_BG_SELECTOR, "(TexturePath: \"\")");
			return;
		}

		b.set(AMMO_ICON_BG_SELECTOR, ammoIcon);
		b.set(AMMO_ICON_VISIBLE_SELECTOR, true);
	}

	private static void syncWeaponIcon(UICommandBuilder b, @Nullable String weaponIcon) {
		if (weaponIcon == null || weaponIcon.isBlank()) {
			b.set(WEAPON_ICON_VISIBLE_SELECTOR, false);
			b.set(WEAPON_ICON_BG_SELECTOR, "(TexturePath: \"\")");
			return;
		}

		b.set(WEAPON_ICON_BG_SELECTOR, weaponIcon);
		b.set(WEAPON_ICON_VISIBLE_SELECTOR, true);
	}

	@Override
	public String id() {
		return SCREEN_ID;
	}

	@Override
	public int zIndex() {
		return 100;
	}

	@Override
	public Class<State> stateType() {
		return State.class;
	}

	@Override
	public void build(@Nullable State state, UICommandBuilder uiCommandBuilder) {
		uiCommandBuilder.append(AMMO_UI_PATH);
		apply(
			uiCommandBuilder, state == null
			                  ? HIDDEN
			                  : state, state != null
		);
	}

	@Override
	public boolean patch(@Nullable State previousState, @Nullable State nextState, UICommandBuilder uiCommandBuilder) {
		apply(
			uiCommandBuilder, nextState == null
			                  ? HIDDEN
			                  : nextState, nextState != null
		);
		return true;
	}

	public record State(
		String itemId, int ammo, int maxAmmo, int reserveAmmo, boolean reloading, @Nullable String ammoIcon,
		@Nullable String weaponIcon
	) {
	}
}
