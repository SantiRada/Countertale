package com.thescar.hygunsplugin.support.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.StatModifiersManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Suppresses queued Signature stat clears triggered by inventory slot rewrites.
 *
 * <p>
 * When active slot metadata changes, server inventory listeners can enqueue
 * EntityStatsToClear from the weapon. We remove Signature-related entries from
 * that queue after our own item writes so normal ChangeStat interactions still
 * control Signature values (including ultimate costs).
 * </p>
 */
public final class SignatureStatsGuard {
	private static final String SIGNATURE_ENERGY = "SignatureEnergy";
	private static final String SIGNATURE_CHARGES = "SignatureCharges";
	private static final Field STATS_TO_CLEAR_FIELD;

	static {
		Field field = null;
		try {
			field = StatModifiersManager.class.getDeclaredField("statsToClear");
			field.setAccessible(true);
		} catch (Throwable ignored) {
			field = null;
		}

		STATS_TO_CLEAR_FIELD = field;
	}

	private SignatureStatsGuard() {
	}

	public static void preventQueuedReset(@Nullable Ref<EntityStore> ref) {
		try {
			if (STATS_TO_CLEAR_FIELD == null || ref == null || !ref.isValid()) {
				return;
			}

			EntityStatMap statMap = getStatMap(ref);
			if (statMap == null) {
				return;
			}

			StatModifiersManager manager = statMap.getStatModifiersManager();
			if (manager == null) {
				return;
			}

			EntityStatValue energy = getStat(statMap, SIGNATURE_ENERGY);
			EntityStatValue charges = getStat(statMap, SIGNATURE_CHARGES);
			Object raw = STATS_TO_CLEAR_FIELD.get(manager);
			if (!(raw instanceof IntSet statsToClear)) {
				return;
			}

			if (energy != null) {
				statsToClear.remove(energy.getIndex());
			}

			if (charges != null) {
				statsToClear.remove(charges.getIndex());
			}

		} catch (Throwable ignored) {
			// Best-effort safeguard: if reflection fails, we don't break gameplay.
		}
	}

	@Nullable
	private static EntityStatMap getStatMap(@Nullable Ref<EntityStore> ref) {
		if (ref == null || !ref.isValid()) {
			return null;
		}

		try {
			Store<EntityStore> store = ref.getStore();
			if (store == null) {
				return null;
			}

			return store.getComponent(ref, EntityStatMap.getComponentType());
		} catch (Throwable ignored) {
			return null;
		}
	}

	@Nullable
	private static EntityStatValue getStat(@Nonnull EntityStatMap statMap, @Nonnull String id) {
		int index = EntityStatType.getAssetMap().getIndexOrDefault(id, -1);
		return index >= 0
		       ? statMap.get(index)
		       : null;
	}
}
