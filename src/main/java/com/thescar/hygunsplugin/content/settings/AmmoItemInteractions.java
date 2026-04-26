package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AmmoItemInteractions(List<JsonObject> entityHit, List<JsonObject> blockHit, List<JsonObject> miss) {
	public AmmoItemInteractions() {
		this(List.of(), List.of(), List.of());
	}

	public AmmoItemInteractions(List<JsonObject> entityHit, List<JsonObject> blockHit, List<JsonObject> miss) {
		this.entityHit = List.copyOf(entityHit);
		this.blockHit = List.copyOf(blockHit);
		this.miss = List.copyOf(miss);
	}

	public static AmmoItemInteractions fromJson(@Nullable JsonObject settings) {
		if (settings == null) {
			return new AmmoItemInteractions();
		}

		JsonObject interactions = JsonValueUtils.Read.object(settings, "Interactions");
		if (interactions == null) {
			return new AmmoItemInteractions();
		}

		return new AmmoItemInteractions(
			readInteractionList(interactions.get("EntityHit")),
			readInteractionList(interactions.get("BlockHit")), readInteractionList(interactions.get("Miss"))
		);
	}

	private static List<JsonObject> readInteractionList(@Nullable JsonElement raw) {
		if (raw == null || !raw.isJsonArray()) {
			return List.of();
		}

		JsonArray array = raw.getAsJsonArray();
		List<JsonObject> out = new ArrayList<>(array.size());
		for (JsonElement element : array) {
			if (element != null && element.isJsonObject()) {
				out.add(element.getAsJsonObject().deepCopy());
			}
		}

		return out;
	}


	public boolean hasAnyValue() {
		return !this.entityHit.isEmpty() || !this.blockHit.isEmpty() || !this.miss.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoItemInteractions(
			List<JsonObject> hit, List<JsonObject> blockHit1, List<JsonObject> miss1
		))) {
			return false;
		}

		return Objects.equals(this.entityHit, hit) && Objects.equals(this.blockHit, blockHit1)
			&& Objects.equals(this.miss, miss1);
	}

}
