package com.thescar.hygunsplugin.runtime.item.core;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.support.hytale.ItemStackUtils;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import org.bson.BsonDocument;
import org.bson.BsonString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class RuntimeItemIdentity {
	public static final String RUNTIME_DATA_KEY = HygunsPluginMain.key("RuntimeData");
	public static final String RUNTIME_ITEM_ID_FIELD = "RuntimeItemId";

	private RuntimeItemIdentity() {
	}

	@Nullable
	public static RuntimeItemRef resolve(@Nullable ItemStack itemStack) {
		BsonDocument runtimeData = ItemStackUtils.getCustomDocument(itemStack, RUNTIME_DATA_KEY);
		String raw = runtimeData != null && runtimeData.containsKey(RUNTIME_ITEM_ID_FIELD)
						 && runtimeData.get(RUNTIME_ITEM_ID_FIELD).isString()
		             ? runtimeData.getString(RUNTIME_ITEM_ID_FIELD).getValue()
		             : null;
		if (raw == null || raw.isBlank()) {
			return null;
		}

		try {
			return new RuntimeItemRef(UUID.fromString(raw));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	@Nonnull
	public static Assignment ensure(@Nonnull ItemStack itemStack) {
		RuntimeItemRef existing = resolve(itemStack);
		if (existing != null) {
			return new Assignment(existing, itemStack, false);
		}

		RuntimeItemRef created = new RuntimeItemRef(UUID.randomUUID());
		BsonDocument runtimeData = ItemStackUtils.getCustomDocument(itemStack, RUNTIME_DATA_KEY);
		BsonDocument updatedData = runtimeData != null
		                           ? runtimeData.clone()
		                           : new BsonDocument();
		updatedData.put(RUNTIME_ITEM_ID_FIELD, new BsonString(created.asString()));
		ItemStack updated = ItemStackUtils.setCustomDocument(itemStack, RUNTIME_DATA_KEY, updatedData);
		return new Assignment(created, updated, true);
	}

	public record Assignment(@Nonnull RuntimeItemRef ref, @Nonnull ItemStack stack, boolean created) {
	}
}
