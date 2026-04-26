package com.thescar.hygunsplugin.runtime.item.core;

import javax.annotation.Nonnull;
import java.util.UUID;

public record RuntimeItemRef(@Nonnull UUID id) {
	public RuntimeItemRef {
		if (id == null) {
			throw new IllegalArgumentException("Runtime item id cannot be null");
		}
	}

	@Nonnull

	public String asString() {
		return this.id.toString();
	}
}
