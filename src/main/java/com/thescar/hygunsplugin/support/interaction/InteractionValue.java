package com.thescar.hygunsplugin.support.interaction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class InteractionValue<T> {
	private final T defaultValue;
	private final List<Predicate<T>> validators;
	private T value;
	private boolean explicitlySet;

	public InteractionValue(T defaultValue) {
		this.defaultValue = defaultValue;
		this.value = defaultValue;
		this.explicitlySet = false;
		this.validators = new ArrayList<>();
	}

	public static <O, T> BiConsumer<O, T> setter(Function<O, InteractionValue<T>> accessor) {
		return (owner, value) -> accessor.apply(owner).set(value);
	}

	public static <O, T> Function<O, T> getter(Function<O, InteractionValue<T>> accessor) {
		return owner -> accessor.apply(owner).get();
	}

	public static <O, T> BiConsumer<O, O> inheritor(Function<O, InteractionValue<T>> accessor) {
		return (owner, parent) -> accessor.apply(owner).set(accessor.apply(parent).get());
	}

	public static <O, T> Binding<O, T> binding(Function<O, InteractionValue<T>> accessor) {
		return new Binding<>(accessor);
	}

	public InteractionValue<T> addValidator(Predicate<T> validator) {
		if (validator == null) {
			throw new IllegalArgumentException("validator cannot be null");
		}

		this.validators.add(validator);
		if (!isValid(this.value)) {
			if (isValid(this.defaultValue)) {
				this.value = this.defaultValue;
				this.explicitlySet = false;
			} else {
				throw new IllegalStateException("default value does not satisfy validators");
			}
		}

		return this;
	}

	public InteractionValue<T> addNullableValidator(Predicate<T> validator) {
		if (validator == null) {
			throw new IllegalArgumentException("validator cannot be null");
		}

		return addValidator(value -> value == null || validator.test(value));
	}

	public void set(T value) {
		if (!isValid(value)) {
			return;
		}

		this.value = value;
		this.explicitlySet = true;
	}

	public T get() {
		return this.value;
	}

	public T get(@Nullable T settingsValue) {
		if (this.explicitlySet && isValid(this.value)) {
			return this.value;
		}

		if (settingsValue != null && isValid(settingsValue)) {
			return settingsValue;
		}

		if (isValid(this.value)) {
			return this.value;
		}

		if (isValid(this.defaultValue)) {
			return this.defaultValue;
		}

		throw new IllegalStateException("no valid value available for InteractionValue");
	}

	public <S> T get(@Nullable S settings, Function<S, T> settingsExtractor) {
		if (this.explicitlySet && isValid(this.value)) {
			return this.value;
		}

		if (settings != null) {
			T settingsValue = settingsExtractor.apply(settings);
			if (settingsValue != null && isValid(settingsValue)) {
				return settingsValue;
			}
		}

		if (isValid(this.value)) {
			return this.value;
		}

		if (isValid(this.defaultValue)) {
			return this.defaultValue;
		}

		throw new IllegalStateException("no valid value available for InteractionValue");
	}

	public boolean isSet() {
		return this.explicitlySet;
	}

	private boolean isValid(T candidate) {
		for (Predicate<T> validator : this.validators) {
			if (!validator.test(candidate)) {
				return false;
			}
		}

		return true;
	}

	public static final class Binding<O, T> {
		private final Function<O, InteractionValue<T>> accessor;

		private Binding(Function<O, InteractionValue<T>> accessor) {
			this.accessor = accessor;
		}

		public BiConsumer<O, T> setter() {
			return InteractionValue.setter(this.accessor);
		}

		public Function<O, T> getter() {
			return InteractionValue.getter(this.accessor);
		}

		public BiConsumer<O, O> inheritor() {
			return InteractionValue.inheritor(this.accessor);
		}

		public InteractionValue<T> value(O owner) {
			return this.accessor.apply(owner);
		}
	}
}
