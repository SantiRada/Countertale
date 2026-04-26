package com.thescar.hygunsplugin.runtime.item.ecs;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class RuntimeStore<S> {
	private final S externalData;
	private final List<RuntimeEntityTickingSystem<S>> systems;
	private final ConcurrentHashMap<UUID, EntityRecord<S>> entities = new ConcurrentHashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	RuntimeStore(@Nonnull S externalData, @Nonnull List<RuntimeEntityTickingSystem<S>> systems) {
		this.externalData = Objects.requireNonNull(externalData, "externalData");
		this.systems = Objects.requireNonNull(systems, "systems");
	}

	@Nonnull
	public S getExternalData() {
		return externalData;
	}

	@Nonnull
	public RuntimeEntityRef<S> ensureEntity(@Nonnull UUID entityId) {
		lock.writeLock().lock();
		try {
			entities.computeIfAbsent(entityId, EntityRecord::new);
			return new RuntimeEntityRef<>(entityId);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public boolean contains(@Nonnull RuntimeEntityRef<S> ref) {
		lock.readLock().lock();
		try {
			return entities.containsKey(ref.entityId());
		} finally {
			lock.readLock().unlock();
		}
	}

	public void removeEntity(@Nonnull RuntimeEntityRef<S> ref) {
		lock.writeLock().lock();
		try {
			entities.remove(ref.entityId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Nullable
	public <T extends Component<S>> T getComponent(@Nonnull RuntimeEntityRef<S> ref,
	                                               @Nonnull RuntimeComponentType<S, T> componentType) {
		lock.readLock().lock();
		try {
			EntityRecord<S> record = entities.get(ref.entityId());
			if (record == null) {
				return null;
			}
			Component<S> component = record.components.get(componentType);
			return componentType.componentClass().isInstance(component)
			       ? componentType.componentClass().cast(component)
			       : null;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Nonnull
	public <T extends Component<S>> T ensureAndGetComponent(@Nonnull RuntimeEntityRef<S> ref,
	                                                        @Nonnull RuntimeComponentType<S, T> componentType) {
		lock.writeLock().lock();
		try {
			EntityRecord<S> record = entities.computeIfAbsent(ref.entityId(), EntityRecord::new);
			Component<S> component = record.components.computeIfAbsent(componentType, ignored -> componentType.create());
			return componentType.componentClass().cast(component);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public <T extends Component<S>> void removeComponent(@Nonnull RuntimeEntityRef<S> ref,
	                                                     @Nonnull RuntimeComponentType<S, T> componentType) {
		lock.writeLock().lock();
		try {
			EntityRecord<S> record = entities.get(ref.entityId());
			if (record == null) {
				return;
			}
			record.components.remove(componentType);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void setOwnerWorld(@Nonnull RuntimeEntityRef<S> ref, @Nullable String ownerWorldName) {
		lock.writeLock().lock();
		try {
			EntityRecord<S> record = entities.computeIfAbsent(ref.entityId(), EntityRecord::new);
			record.ownerWorldName = ownerWorldName;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Nullable
	public String ownerWorld(@Nonnull RuntimeEntityRef<S> ref) {
		lock.readLock().lock();
		try {
			EntityRecord<S> record = entities.get(ref.entityId());
			return record != null ? record.ownerWorldName : null;
		} finally {
			lock.readLock().unlock();
		}
	}

	public void tick(float dt, @Nullable String worldName) {
		for (RuntimeEntityTickingSystem<S> system : systems) {
			List<RuntimeEntityRef<S>> matchingRefs = matchingRefs(system.getQuery(), worldName);
			if (matchingRefs.isEmpty()) {
				continue;
			}
			RuntimeArchetypeChunk<S> chunk = new RuntimeArchetypeChunk<>(this, matchingRefs);
			RuntimeCommandBuffer<S> commandBuffer = new RuntimeCommandBuffer<>(this);
			for (int index = 0; index < chunk.size(); index++) {
				system.tick(dt, index, chunk, this, commandBuffer);
			}
			commandBuffer.flush();
		}
	}

	@Nonnull
	public List<RuntimeEntityRef<S>> refs(@Nonnull RuntimeQuery<S> query, @Nullable String worldName) {
		return matchingRefs(query, worldName);
	}

	@Nonnull
	private List<RuntimeEntityRef<S>> matchingRefs(@Nonnull RuntimeQuery<S> query, @Nullable String worldName) {
		lock.readLock().lock();
		try {
			List<RuntimeEntityRef<S>> refs = new ArrayList<>();
			Set<RuntimeComponentType<S, ?>> required = query.required();
			for (EntityRecord<S> record : entities.values()) {
				if (worldName != null && record.ownerWorldName != null && !worldName.equals(record.ownerWorldName)) {
					continue;
				}
				if (!record.components.keySet().containsAll(required)) {
					continue;
				}
				refs.add(new RuntimeEntityRef<>(record.entityId));
			}
			return refs;
		} finally {
			lock.readLock().unlock();
		}
	}

	private static final class EntityRecord<S> {
		private final UUID entityId;
		private final Map<RuntimeComponentType<S, ?>, Component<S>> components = new LinkedHashMap<>();
		private @Nullable String ownerWorldName;

		private EntityRecord(@Nonnull UUID entityId) {
			this.entityId = entityId;
		}
	}
}
