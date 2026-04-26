package com.thescar.hygunsplugin.runtime.item.core;

import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.AmmoItemComponent;
import com.thescar.hygunsplugin.runtime.components.FireDelayComponent;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.components.ItemRuntimeIdentityComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponItemComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponReloadComponent;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentRegistry;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeEntityRef;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeQuery;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeStore;
import com.thescar.hygunsplugin.runtime.systems.HeatTickSystem;
import com.thescar.hygunsplugin.runtime.systems.RuntimeWeaponDirtyTickSystem;
import com.thescar.hygunsplugin.runtime.systems.WeaponFireDelayTickSystem;
import com.thescar.hygunsplugin.runtime.systems.WeaponReloadTickSystem;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ItemRuntimeEcs {
	public static final RuntimeComponentRegistry<ItemRuntimeStore> REGISTRY = new RuntimeComponentRegistry<>();
	public static final RuntimeComponentType<ItemRuntimeStore, ItemRuntimeIdentityComponent> IDENTITY_TYPE = REGISTRY
		.registerComponent(ItemRuntimeIdentityComponent.class, ItemRuntimeIdentityComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, WeaponItemComponent> WEAPON_TYPE = REGISTRY
		.registerComponent(WeaponItemComponent.class, WeaponItemComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, AmmoItemComponent> AMMO_TYPE = REGISTRY
		.registerComponent(AmmoItemComponent.class, AmmoItemComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, AmmoDataComponent> AMMO_DATA_TYPE = REGISTRY
		.registerComponent(AmmoDataComponent.class, AmmoDataComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, FireDelayComponent> FIRE_DELAY_TYPE = REGISTRY
		.registerComponent(FireDelayComponent.class, FireDelayComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, HeatDataComponent> HEAT_DATA_TYPE = REGISTRY
		.registerComponent(HeatDataComponent.class, HeatDataComponent::new);
	public static final RuntimeComponentType<ItemRuntimeStore, WeaponReloadComponent> WEAPON_RELOAD_TYPE = REGISTRY
		.registerComponent(WeaponReloadComponent.class, WeaponReloadComponent::new);

	private static final RuntimeStore<ItemRuntimeStore> STORE = REGISTRY.addStore(new ItemRuntimeStore("global"));
	private static final ConcurrentHashMap<Thread, String> WORLD_BY_THREAD = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap.KeySetView<UUID, Boolean> LOGGED_MISSING_RUNTIMES = ConcurrentHashMap.newKeySet();

	static {
		REGISTRY.registerSystem(new WeaponFireDelayTickSystem());
		REGISTRY.registerSystem(new HeatTickSystem());
		REGISTRY.registerSystem(new WeaponReloadTickSystem());
		REGISTRY.registerSystem(new RuntimeWeaponDirtyTickSystem());
	}

	private ItemRuntimeEcs() {
	}

	public static void rememberWorldThread(@Nullable World world) {
		if (world == null) {
			return;
		}

		String worldName = worldKey(world);
		String previousWorldName = WORLD_BY_THREAD.put(Thread.currentThread(), worldName);
		if (Objects.equals(previousWorldName, worldName)) {
			return;
		}
		DebugLogger.debug(
			"ItemRuntimeEcs", () -> "Bound thread to world: thread="
				+ Thread.currentThread().getName()
				+ ", world=" + worldName
		);
	}

	@Nullable
	public static String currentWorldName() {
		return WORLD_BY_THREAD.get(Thread.currentThread());
	}

	public static void tick(float dt) {
		STORE.tick(dt, currentWorldName());
	}

	public static void clearPlayerInventoryRuntime(@Nullable com.hypixel.hytale.server.core.entity.entities.Player player) {
		DebugLogger.debug("ItemRuntimeEcs", "clearPlayerInventoryRuntime skipped: global runtime store does not migrate by world");
	}

	@Nullable
	public static RuntimeEntityRef<ItemRuntimeStore> resolveEntity(@Nullable RuntimeItemRef runtimeRef) {
		if (runtimeRef == null) {
			return null;
		}

		RuntimeEntityRef<ItemRuntimeStore> ref = new RuntimeEntityRef<>(runtimeRef.id());
		if (!STORE.contains(ref)) {
			if (LOGGED_MISSING_RUNTIMES.add(runtimeRef.id())) {
				DebugLogger.debug("ItemRuntimeEcs", () -> "Runtime not resolved: runtimeId=" + runtimeRef.id());
			}
			return null;
		}

		return ref;
	}

	@Nullable
	public static ItemRuntimeKind kindOf(@Nullable RuntimeItemRef runtimeRef) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = resolveEntity(runtimeRef);
		if (entityRef == null) {
			return null;
		}

		ItemRuntimeIdentityComponent identity = STORE.getComponent(entityRef, IDENTITY_TYPE);
		return identity != null
		       ? identity.kind()
		       : null;
	}

	@Nonnull
	public static RuntimeItemIdentity.Assignment ensureTracked(@Nonnull ItemStack itemStack) {
		RuntimeItemIdentity.Assignment assignment = RuntimeItemIdentity.ensure(itemStack);
		bind(assignment.ref(), assignment.stack());
		return assignment;
	}

	@Nonnull
	public static RuntimeEntityRef<ItemRuntimeStore> bind(@Nonnull RuntimeItemRef runtimeRef, @Nonnull ItemStack itemStack) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = STORE.ensureEntity(runtimeRef.id());
		String currentWorld = currentWorldName();
		if (currentWorld != null) {
			STORE.setOwnerWorld(entityRef, currentWorld);
		}

		ItemRuntimeKind kind = classify(itemStack);
		ItemRuntimeIdentityComponent identity = STORE.ensureAndGetComponent(entityRef, IDENTITY_TYPE);
		identity.setRuntimeId(runtimeRef.id());
		identity.setItemId(itemStack.getItemId());
		identity.setKind(kind);
		syncKindComponents(entityRef, kind);
		DebugLogger.debug(
			"ItemRuntimeEcs", () -> "Bound runtime entity: runtimeId="
				+ runtimeRef.asString()
				+ ", world=" + currentWorld
				+ ", itemId=" + itemStack.getItemId()
		);
		return entityRef;
	}

	public static void remove(@Nullable RuntimeItemRef runtimeRef) {
		if (runtimeRef == null) {
			return;
		}

		RuntimeEntityRef<ItemRuntimeStore> entityRef = new RuntimeEntityRef<>(runtimeRef.id());
		STORE.removeEntity(entityRef);
		DebugLogger.debug("ItemRuntimeEcs", () -> "Removed runtime entity: runtimeId=" + runtimeRef.asString());
	}

	public static void clear() {
		// Global runtime store is intentionally process-lifetime; explicit clear is not
		// used beyond shutdown/test flows.
	}

	@Nullable
	public static <T extends Component<ItemRuntimeStore>> T getComponent(@Nullable RuntimeItemRef runtimeRef,
	                                                                     @Nonnull RuntimeComponentType<ItemRuntimeStore, T> componentType) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = resolveEntity(runtimeRef);
		return entityRef != null
		       ? STORE.getComponent(entityRef, componentType)
		       : null;
	}

	@Nullable
	public static <T extends Component<ItemRuntimeStore>> T getComponent(@Nullable RuntimeEntityRef<ItemRuntimeStore> entityRef,
	                                                                     @Nonnull RuntimeComponentType<ItemRuntimeStore, T> componentType) {
		return entityRef != null
		       ? STORE.getComponent(entityRef, componentType)
		       : null;
	}

	@Nonnull
	public static <T extends Component<ItemRuntimeStore>> T ensureComponent(@Nonnull RuntimeItemRef runtimeRef,
	                                                                        @Nonnull RuntimeComponentType<ItemRuntimeStore, T> componentType) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = bindIfMissing(runtimeRef);
		return STORE.ensureAndGetComponent(entityRef, componentType);
	}

	public static <T extends Component<ItemRuntimeStore>> void removeComponent(@Nonnull RuntimeItemRef runtimeRef,
	                                                                           @Nonnull RuntimeComponentType<ItemRuntimeStore, T> componentType) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = resolveEntity(runtimeRef);
		if (entityRef == null) {
			return;
		}
		STORE.removeComponent(entityRef, componentType);
	}

	@Nonnull
	public static RuntimeQuery<ItemRuntimeStore> queryFor(@Nonnull ItemRuntimeKind kind) {
		return switch (kind) {
			case WEAPON -> RuntimeQuery.and(IDENTITY_TYPE, WEAPON_TYPE);
			case AMMO -> RuntimeQuery.and(IDENTITY_TYPE, AMMO_TYPE);
		};
	}

	public static void forEach(@Nonnull RuntimeQuery<ItemRuntimeStore> query,
	                           @Nonnull BiConsumer<RuntimeItemRef, RuntimeEntityRef<ItemRuntimeStore>> consumer) {
		for (RuntimeEntityRef<ItemRuntimeStore> entityRef : STORE.refs(query, currentWorldName())) {
			ItemRuntimeIdentityComponent identity = STORE.getComponent(entityRef, IDENTITY_TYPE);
			if (identity == null || identity.runtimeId() == null) {
				continue;
			}
			consumer.accept(new RuntimeItemRef(identity.runtimeId()), entityRef);
		}
	}

	public static void forEachKind(@Nonnull ItemRuntimeKind kind,
	                               @Nonnull BiConsumer<RuntimeItemRef, RuntimeEntityRef<ItemRuntimeStore>> consumer) {
		forEach(queryFor(kind), consumer);
	}

	@Nonnull
	private static RuntimeEntityRef<ItemRuntimeStore> bindIfMissing(@Nonnull RuntimeItemRef runtimeRef) {
		RuntimeEntityRef<ItemRuntimeStore> entityRef = resolveEntity(runtimeRef);
		if (entityRef != null) {
			return entityRef;
		}
		RuntimeEntityRef<ItemRuntimeStore> created = STORE.ensureEntity(runtimeRef.id());
		String currentWorld = currentWorldName();
		if (currentWorld != null) {
			STORE.setOwnerWorld(created, currentWorld);
		}
		return created;
	}

	private static void syncKindComponents(@Nonnull RuntimeEntityRef<ItemRuntimeStore> entityRef,
	                                       @Nonnull ItemRuntimeKind kind) {
		STORE.removeComponent(entityRef, WEAPON_TYPE);
		STORE.removeComponent(entityRef, AMMO_TYPE);
		switch (kind) {
			case WEAPON -> STORE.ensureAndGetComponent(entityRef, WEAPON_TYPE);
			case AMMO -> STORE.ensureAndGetComponent(entityRef, AMMO_TYPE);
		}
	}

	@Nonnull
	private static String worldKey(@Nonnull World world) {
		String name = world.getName();
		return name == null || name.isBlank()
		       ? "default"
		       : name;
	}

	@Nonnull
	private static ItemRuntimeKind classify(@Nullable ItemStack itemStack) {
		if (itemStack == null || itemStack.isEmpty()) {
			return ItemRuntimeKind.WEAPON;
		}
		String itemId = itemStack.getItemId();
		if (GunRegistry.getSettings(itemId) != null) {
			return ItemRuntimeKind.WEAPON;
		}
		if (AmmoRegistry.isAmmo(itemId)) {
			return ItemRuntimeKind.AMMO;
		}
		return ItemRuntimeKind.WEAPON;
	}
}
