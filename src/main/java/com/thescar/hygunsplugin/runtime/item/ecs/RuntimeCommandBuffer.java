package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class RuntimeCommandBuffer<S> {
	private final RuntimeStore<S> store;
	private final List<Runnable> operations = new ArrayList<>();

	RuntimeCommandBuffer(@Nonnull RuntimeStore<S> store) {
		this.store = store;
	}

	public <T extends Component<S>> void removeComponent(@Nonnull RuntimeEntityRef<S> ref,
	                                                     @Nonnull RuntimeComponentType<S, T> componentType) {
		operations.add(() -> store.removeComponent(ref, componentType));
	}

	public void removeEntity(@Nonnull RuntimeEntityRef<S> ref) {
		operations.add(() -> store.removeEntity(ref));
	}

	void flush() {
		if (operations.isEmpty()) {
			return;
		}
		List<Runnable> snapshot = new ArrayList<>(operations);
		operations.clear();
		for (Runnable operation : snapshot) {
			operation.run();
		}
	}
}
