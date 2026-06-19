package Tenzinn.Core.Events;

import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DamageStatsTracker extends DamageEventSystem {

    @Nonnull @Override
    public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getInspectDamageGroup(); }

    @Nonnull @Override
    public Query<EntityStore> getQuery() { return Query.and(Player.getComponentType()); }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        if (damage.isCancelled() || damage.getAmount() <= 0) return;

        World defaultWorld = Universe.get().getDefaultWorld();
        World currentWorld = store.getExternalData().getWorld();
        if (defaultWorld == null || defaultWorld == currentWorld) return;

        PlayerRef victimRef = getPlayerRef(chunk.getReferenceTo(index), store);
        if (victimRef == null) return;

        PlayerStats victimStats = RefactorTool.getPlayerStats(victimRef);
        if (victimStats == null) return;

        float amount = damage.getAmount();
        victimStats.addDamageReceived(amount);

        PlayerRef attackerRef = getAttackerRef(damage, store);
        if (attackerRef == null || attackerRef.getUuid().equals(victimRef.getUuid())) return;

        PlayerStats attackerStats = RefactorTool.getPlayerStats(attackerRef);
        if (attackerStats == null || attackerStats.getCurrentMatch() != victimStats.getCurrentMatch()) return;

        attackerStats.addDamageCaused(amount, isMeleeDamage(damage));
    }

    private PlayerRef getAttackerRef(Damage damage, Store<EntityStore> store) {
        if (damage.getSource() instanceof Damage.ProjectileSource projectileSource) {
            return getPlayerRef(projectileSource.getRef(), store);
        }
        if (damage.getSource() instanceof Damage.EntitySource entitySource) {
            return getPlayerRef(entitySource.getRef(), store);
        }
        return null;
    }

    private PlayerRef getPlayerRef(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid()) return null;
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        return uuidComponent != null ? Universe.get().getPlayer(uuidComponent.getUuid()) : null;
    }

    private boolean isMeleeDamage(Damage damage) {
        if (!(damage.getSource() instanceof Damage.EntitySource)) return false;
        if (damage.getSource() instanceof Damage.ProjectileSource) return false;

        DamageCause cause = damage.getCause();
        return cause == null || DamageCause.PHYSICAL.equals(cause)
                || "physical".equalsIgnoreCase(cause.getId());
    }
}
