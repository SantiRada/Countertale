package com.thescar.hygunsplugin.content.ammo;

import com.thescar.hygunsplugin.content.settings.AmmoItemSettings;

import javax.annotation.Nullable;
import java.util.Objects;

public record AmmoDefinition(String itemId, @Nullable AmmoItemSettings settings) {
	public boolean hasAnyValue() {
		return settings != null && settings.hasAnyValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoDefinition(String id, AmmoItemSettings settings1))) {
			return false;
		}

		return Objects.equals(this.itemId, id) && Objects.equals(this.settings, settings1);
	}
}
