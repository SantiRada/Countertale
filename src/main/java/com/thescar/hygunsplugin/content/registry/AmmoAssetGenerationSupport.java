package com.thescar.hygunsplugin.content.registry;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

final class AmmoAssetGenerationSupport {
	private static final String LEGACY_PILE_ICON_PREFIX = "Icons/ItemGenerated/";

	private AmmoAssetGenerationSupport() {
	}

	static String normalizeIconPath(String icon, String fallbackIcon) {
		if (icon == null || icon.isBlank()) {
			return fallbackIcon;
		}

		String normalized = icon.trim();
		if (normalized.startsWith(LEGACY_PILE_ICON_PREFIX)) {
			return "Icons/ItemsGenerated/" + normalized.substring(LEGACY_PILE_ICON_PREFIX.length());
		}

		return normalized;
	}

	static String resolveAmmoDisplayName(String ammoItemId) {
		Item item = resolveAmmoItemAsset(ammoItemId);
		if (item == null) {
			return humanizeAmmoType(ammoItemId);
		}

		String translationKey = item.getTranslationKey();
		if (translationKey == null || translationKey.isBlank()) {
			return humanizeAmmoType(ammoItemId);
		}

		I18nModule i18n = I18nModule.get();
		if (i18n == null) {
			return humanizeAmmoType(ammoItemId);
		}

		String translated = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, translationKey);
		return translated == null || translated.isBlank() || translated.equals(translationKey)
		       ? humanizeAmmoType(ammoItemId)
		       : translated;
	}

	static String resolveAmmoItemIdForAssets(String ammoItemId) {
		Item item = resolveAmmoItemAsset(ammoItemId);
		if (item != null) {
			String id = item.getId();
			if (id != null && !id.isBlank() && item != Item.UNKNOWN) {
				return id;
			}
		}

		String normalized = ammoItemId.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		return normalized;
	}

	static String ammoTypeId(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		return sanitizeIdToken(normalized);
	}

	static String sanitizeIdToken(String value) {
		StringBuilder out = new StringBuilder(value.length());
		boolean lastWasUnderscore = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			boolean safe = c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| c >= '0' && c <= '9'
				|| c == '_';
			if (safe) {
				out.append(c);
				lastWasUnderscore = false;
				continue;
			}

			if (!lastWasUnderscore) {
				out.append('_');
				lastWasUnderscore = true;
			}
		}

		String sanitized = out.toString().replaceAll("^_+|_+$", "");
		return sanitized.isBlank()
		       ? "Unknown"
		       : sanitized;
	}

	static String humanizeToken(String value) {
		String humanized = value.replace('\\', ' ')
			.replace('/', ' ')
			.replace('_', ' ')
			.replace('-', ' ')
			.trim()
			.replaceAll("\\s+", " ");
		return humanized.isBlank()
		       ? "Unknown"
		       : humanized;
	}

	private static String humanizeAmmoType(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		if (normalized.regionMatches(true, 0, "Ammo_", 0, "Ammo_".length())) {
			normalized = normalized.substring("Ammo_".length());
		}

		return humanizeToken(normalized);
	}

	private static Item resolveAmmoItemAsset(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		Item exact = Item.getAssetMap().getAsset(normalized);
		if (exact != null && exact != Item.UNKNOWN) {
			return exact;
		}

		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			String stripped = normalized.substring(colonIndex + 1);
			Item strippedItem = Item.getAssetMap().getAsset(stripped);
			if (strippedItem != null && strippedItem != Item.UNKNOWN) {
				return strippedItem;
			}
		}

		return null;
	}
}
