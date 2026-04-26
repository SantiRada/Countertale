package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public final class RuntimeComponentType<S, T extends Component<S>> {
	private final Class<T> componentClass;
	private final Supplier<T> factory;

	RuntimeComponentType(@Nonnull Class<T> componentClass, @Nonnull Supplier<T> factory) {
		this.componentClass = Objects.requireNonNull(componentClass, "componentClass");
		this.factory = Objects.requireNonNull(factory, "factory");
	}

	@Nonnull
	public Class<T> componentClass() {
		return componentClass;
	}

	@Nonnull
	public T create() {
		return factory.get();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RuntimeComponentType<?, ?> other)) {
			return false;
		}
		return componentClass.equals(other.componentClass);
	}

	@Override
	public int hashCode() {
		return componentClass.hashCode();
	}
}
