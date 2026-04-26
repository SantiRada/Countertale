package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class RuntimeArchetypeChunk<S> {
	private final RuntimeStore<S> store;
	private final List<RuntimeEntityRef<S>> refs;

	RuntimeArchetypeChunk(@Nonnull RuntimeStore<S> store, @Nonnull List<RuntimeEntityRef<S>> refs) {
		this.store = store;
		this.refs = refs;
	}

	public int size() {
		return refs.size();
	}

	@Nullable
	public <T extends Component<S>> T getComponent(int index, @Nonnull RuntimeComponentType<S, T> componentType) {
		if (index < 0 || index >= refs.size()) {
			return null;
		}
		return store.getComponent(refs.get(index), componentType);
	}

	@Nonnull
	public RuntimeEntityRef<S> getReferenceTo(int index) {
		return refs.get(index);
	}
}
