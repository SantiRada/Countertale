package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RuntimeQuery<S> {
	private final Set<RuntimeComponentType<S, ?>> required;

	private RuntimeQuery(@Nonnull Set<RuntimeComponentType<S, ?>> required) {
		this.required = Collections.unmodifiableSet(required);
	}

	@SafeVarargs
	@Nonnull
	public static <S> RuntimeQuery<S> and(@Nonnull RuntimeComponentType<S, ? extends Component<S>>... required) {
		LinkedHashSet<RuntimeComponentType<S, ?>> set = new LinkedHashSet<>(Arrays.asList(required));
		return new RuntimeQuery<>(set);
	}

	@Nonnull
	public Set<RuntimeComponentType<S, ?>> required() {
		return required;
	}
}
