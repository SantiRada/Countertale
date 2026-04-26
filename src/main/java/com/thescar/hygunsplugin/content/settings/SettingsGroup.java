package com.thescar.hygunsplugin.content.settings;

import com.hypixel.hytale.codec.Codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SettingsGroup<T> {
	private final @Nullable String key;
	private final Supplier<T> factory;
	private final List<Field<T, ?>> fields;

	private SettingsGroup(@Nullable String key, Supplier<T> factory) {
		this.key = key;
		this.factory = factory;
		this.fields = new ArrayList<>();
	}

	public static <T> SettingsGroup<T> inline(Supplier<T> factory) {
		return new SettingsGroup<>(null, factory);
	}

	public static <T> SettingsGroup<T> nested(String key, Supplier<T> factory) {
		return new SettingsGroup<>(key, factory);
	}

	@SuppressWarnings("unchecked")
	private static <T, V> void applyFromJson(T current, Field<T, V> field, JsonObject source) {
		V value = field.read(source);
		if (value != null) {
			field.set(current, value);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T, V> void applyFromValue(T current, Field<T, V> field, @Nullable T primary, @Nullable T fallback) {
		V value = field.get(primary);
		if (value == null) {
			value = field.get(fallback);
		}

		if (value != null) {
			field.set(current, value);
		}
	}

	private static @Nullable JsonObject readNested(@Nullable JsonObject root, String key) {
		if (root == null) {
			return null;
		}

		JsonElement element = root.get(key);
		return element != null && element.isJsonObject()
		       ? element.getAsJsonObject()
		       : null;
	}

	@SuppressWarnings("unchecked")
	private static <V> Codec<V> castCodec(Codec<?> codec) {
		return (Codec<V>) codec;
	}

	public @Nullable String key() {
		return this.key;
	}

	public T create() {
		return this.factory.get();
	}

	public List<Field<T, ?>> fields() {
		return List.copyOf(this.fields);
	}

	public <V> Field<T, V> field(String key, Codec<?> codec, JsonReadKind reader, Function<T, V> getter, BiConsumer<T, V> setter,
	                             String... aliases) {
		Field<T, V> field = new Field<>(this, key, castCodec(codec), reader, getter, setter, List.copyOf(Arrays.asList(aliases)));
		this.fields.add(field);
		return field;
	}

	public T read(@Nullable JsonObject root) {
		JsonObject source = this.key == null
		                    ? root
		                    : readNested(root, this.key);
		T value = this.create();
		if (source == null) {
			return value;
		}

		for (Field<T, ?> field : this.fields) {
			applyFromJson(value, field, source);
		}

		return value;
	}

	public boolean hasAnyValue(@Nullable T value) {
		if (value == null) {
			return false;
		}

		for (Field<T, ?> field : this.fields) {
			if (field.get(value) != null) {
				return true;
			}
		}

		return false;
	}

	public T merge(@Nullable T primary, @Nullable T fallback) {
		T merged = this.create();
		for (Field<T, ?> field : this.fields) {
			applyFromValue(merged, field, primary, fallback);
		}

		return merged;
	}

	public T merge(@Nullable T primary, @Nullable T secondary, @Nullable T fallback) {
		return merge(primary, merge(secondary, fallback));
	}

	public static final class Field<T, V> {
		private final SettingsGroup<T> group;

		private final String key;
		private final Codec<V> codec;
		private final JsonReadKind reader;
		private final Function<T, V> getter;
		private final BiConsumer<T, V> setter;
		private final List<String> aliases;

		private Field(SettingsGroup<T> group, String key, Codec<V> codec, JsonReadKind reader, Function<T, V> getter,
		              BiConsumer<T, V> setter, List<String> aliases) {
			this.group = group;
			this.key = key;
			this.codec = codec;
			this.reader = reader;
			this.getter = getter;
			this.setter = setter;
			this.aliases = aliases;
		}

		public String key() {
			return this.key;
		}

		public Codec<V> codec() {
			return this.codec;
		}

		public List<String> aliases() {
			return this.aliases;
		}

		public @Nullable V get(@Nullable T owner) {
			if (owner == null) {
				return null;
			}

			return this.getter.apply(owner);
		}

		public void set(T owner, V value) {
			this.setter.accept(owner, value);
		}

		public T createValue() {
			return this.group.create();
		}

		public Field<T, V> alias(String aliasKey) {
			return new Field<>(this.group, aliasKey, this.codec, this.reader, this.getter, this.setter, List.of());
		}

		private @Nullable V read(@Nullable JsonObject root) {
			JsonElement element = root != null
			                      ? root.get(this.key)
			                      : null;
			if (element == null) {
				for (String alias : this.aliases) {
					element = root != null
					          ? root.get(alias)
					          : null;
					if (element != null) {
						break;
					}
				}
			}

			@SuppressWarnings("unchecked")
			V value = (V) this.reader.read(element);
			return value;
		}
	}
}
