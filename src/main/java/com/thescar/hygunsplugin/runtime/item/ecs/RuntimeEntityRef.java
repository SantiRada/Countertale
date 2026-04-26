package com.thescar.hygunsplugin.runtime.item.ecs;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public final class RuntimeEntityRef<S> {
	private final UUID entityId;

	public RuntimeEntityRef(@Nonnull UUID entityId) {
		this.entityId = Objects.requireNonNull(entityId, "entityId");
	}

	@Nonnull
	public UUID entityId() {
		return entityId;
	}
}
