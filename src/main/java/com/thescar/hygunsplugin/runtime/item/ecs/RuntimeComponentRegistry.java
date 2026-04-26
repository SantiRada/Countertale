package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class RuntimeComponentRegistry<S> {
	private final List<RuntimeEntityTickingSystem<S>> systems = new CopyOnWriteArrayList<>();

	@Nonnull
	public <T extends Component<S>> RuntimeComponentType<S, T> registerComponent(@Nonnull Class<T> componentClass,
	                                                                             @Nonnull Supplier<T> factory) {
		return new RuntimeComponentType<>(componentClass, factory);
	}

	public void registerSystem(@Nonnull RuntimeEntityTickingSystem<S> system) {
		systems.add(system);
	}

	@Nonnull
	public RuntimeStore<S> addStore(@Nonnull S externalData) {
		return new RuntimeStore<>(externalData, systems);
	}
}
