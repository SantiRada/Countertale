package com.thescar.hygunsplugin.support.hytale;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;

import javax.annotation.Nullable;

public final class ItemStackUtils {
	private ItemStackUtils() {
	}

	private static ItemStack setCustomData(@Nullable ItemStack stack, @Nullable String key, @Nullable BsonValue value) {
		if (stack == null || key == null) {
			return stack;
		}

		return stack.withMetadata(key, value);
	}

	@Nullable
	public static BsonDocument getMetadataDocument(@Nullable ItemStack stack) {
		if (stack == null) {
			return null;
		}

		ItemWithAllMetadata packet = stack.toPacket();
		if (packet == null || packet.metadata == null || packet.metadata.isBlank()) {
			return null;
		}

		try {
			return BsonDocument.parse(packet.metadata);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@Nullable
	private static BsonValue getCustomData(@Nullable ItemStack stack, @Nullable String key) {
		if (stack == null || key == null) {
			return null;
		}

		BsonDocument meta = getMetadataDocument(stack);
		if (meta == null) {
			return null;
		}

		return meta.get(key);
	}

	public static ItemStack setCustomString(@Nullable ItemStack stack, @Nullable String key, @Nullable String value) {
		return setCustomData(
			stack, key, (value != null)
			            ? new BsonString(value)
			            : null
		);
	}

	public static ItemStack setCustomDocument(@Nullable ItemStack stack, @Nullable String key, @Nullable BsonDocument value) {
		return setCustomData(stack, key, value);
	}

	@Nullable
	public static String getCustomString(@Nullable ItemStack stack, @Nullable String key) {
		BsonValue val = getCustomData(stack, key);
		return (val != null && val.isString())
		       ? val.asString().getValue()
		       : null;
	}

	@Nullable
	public static BsonDocument getCustomDocument(@Nullable ItemStack stack, @Nullable String key) {
		BsonValue val = getCustomData(stack, key);
		return (val != null && val.isDocument())
		       ? val.asDocument()
		       : null;
	}

	public static ItemStack setCustomInt(@Nullable ItemStack stack, @Nullable String key, @Nullable Integer value) {
		return setCustomData(
			stack, key, (value != null)
			            ? new BsonInt32(value)
			            : null
		);
	}

	@Nullable
	public static Integer getCustomInt(@Nullable ItemStack stack, @Nullable String key) {
		BsonValue val = getCustomData(stack, key);
		return (val != null && val.isInt32())
		       ? val.asInt32().getValue()
		       : null;
	}
}
