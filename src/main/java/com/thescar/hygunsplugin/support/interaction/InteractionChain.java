package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.content.settings.SettingsGroup;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class InteractionChain<O> {
	private final Class<O> ownerType;
	private final Supplier<O> ownerFactory;
	private final BuilderCodec.Builder<O> builder;
	private final List<java.util.function.BiConsumer<O, O>> mergers;

	private InteractionChain(Class<O> ownerType, Supplier<O> ownerFactory, BuilderCodec.Builder<O> builder) {
		this.ownerType = ownerType;
		this.ownerFactory = ownerFactory;
		this.builder = builder;
		this.mergers = new ArrayList<>();
	}

	public static <O> InteractionChain<O> of(Class<O> ownerType, Supplier<O> ownerFactory, BuilderCodec.Builder<O> builder) {
		return new InteractionChain<>(ownerType, ownerFactory, builder);
	}

	private static <O, V> void mergeValue(O target, O source, Function<O, InteractionValue<V>> accessor) {
		InteractionValue<V> sourceRef = accessor.apply(source);
		if (!sourceRef.isSet()) {
			return;
		}

		accessor.apply(target).set(sourceRef.get());
	}

	public <V> InteractionChain<O> field(SettingsGroup.Field<?, V> field, Function<O, InteractionValue<V>> accessor) {
		this.builder.append(
			new KeyedCodec<>(field.key(), field.codec()), (owner, value) -> accessor.apply(owner).set(value),
			owner -> accessor.apply(owner).get()
		).add();
		this.mergers.add((target, source) -> mergeValue(target, source, accessor));
		return this;
	}

	public <V> InteractionChain<O> inheritedField(SettingsGroup.Field<?, V> field, Function<O, InteractionValue<V>> accessor) {
		this.builder.appendInherited(
			new KeyedCodec<>(field.key(), field.codec()), (owner, value) -> accessor.apply(owner).set(value),
			owner -> accessor.apply(owner).get(), (owner, parent) -> accessor
				.apply(owner)
				.set(accessor.apply(parent).get())
		).add();
		this.mergers.add((target, source) -> mergeValue(target, source, accessor));
		return this;
	}

	public <V> InteractionChain<O> field(String key, Codec<V> codec, Function<O, InteractionValue<V>> accessor) {
		this.builder
			.append(
				new KeyedCodec<>(key, codec), (owner, value) -> accessor.apply(owner).set(value), owner -> accessor
					.apply(owner)
					.get()
			)
			.add();
		this.mergers.add((target, source) -> mergeValue(target, source, accessor));
		return this;
	}

	public <V> InteractionChain<O> inheritedField(String key, Codec<V> codec, Function<O, InteractionValue<V>> accessor) {
		this.builder.appendInherited(
			new KeyedCodec<>(key, codec), (owner, value) -> accessor.apply(owner).set(value),
			owner -> accessor.apply(owner).get(), (owner, parent) -> accessor
				.apply(owner)
				.set(accessor.apply(parent).get())
		).add();
		this.mergers.add((target, source) -> mergeValue(target, source, accessor));
		return this;
	}

	public <T> InteractionChain<O> group(SettingsGroup<T> group, Function<O, InteractionValue<T>> accessor) {
		String key = group.key();
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Nested interaction group requires a key");
		}

		return this.nested(
			key, nested -> {
				for (SettingsGroup.Field<T, ?> field : group.fields()) {
					nested.groupField(field, accessor);
					for (String alias : field.aliases()) {
						nested.groupField(field.alias(alias), accessor);
					}
				}

			}
		);
	}

	public InteractionChain<O> nested(String key, Consumer<InteractionChain<O>> consumer) {
		InteractionChain<O> nested = new InteractionChain<>(
			this.ownerType, this.ownerFactory,
			BuilderCodec.builder(this.ownerType, this.ownerFactory)
		);
		consumer.accept(nested);
		this.builder.append(new KeyedCodec<>(key, nested.build()), nested::mergeInto, owner -> owner).add();
		this.mergers.add(nested::mergeInto);
		return this;
	}

	public InteractionChain<O> documentation(String text) {
		this.builder.documentation(text);
		return this;
	}

	public BuilderCodec<O> build() {
		return this.builder.build();
	}

	private void mergeInto(O target, O source) {
		for (java.util.function.BiConsumer<O, O> merger : this.mergers) {
			merger.accept(target, source);
		}
	}

	private <T, V> void groupField(SettingsGroup.Field<T, V> field, Function<O, InteractionValue<T>> accessor) {
		this.builder.append(
			new KeyedCodec<>(field.key(), field.codec()), (owner, value) -> {
				InteractionValue<T> ref = accessor.apply(owner);
				T target = ref.get();
				if (target == null) {
					target = field.createValue();
				}

				field.set(target, value);
				ref.set(target);
			}, owner -> field.get(accessor.apply(owner).get())
		).add();
		this.mergers.add((target, source) -> {
			InteractionValue<T> sourceRef = accessor.apply(source);
			V value = field.get(sourceRef.get());
			if (value == null) {
				return;
			}

			InteractionValue<T> targetRef = accessor.apply(target);
			T targetValue = targetRef.get();
			if (targetValue == null) {
				targetValue = field.createValue();
			}

			field.set(targetValue, value);
			targetRef.set(targetValue);
		});
	}
}
