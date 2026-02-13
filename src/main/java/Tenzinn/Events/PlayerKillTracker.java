package Tenzinn.Events;

import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Objects;

public class PlayerKillTracker extends DamageEventSystem {

    @Override
    public Query<EntityStore> getQuery() { return Query.any(); }

    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        Player victim = store.getComponent(victimRef, Player.getComponentType());

        if (victim == null) return;
        Damage.Source source = damage.getSource();

        if (!(source instanceof Damage.EntitySource)) {
            // Muertes por el mundo
            RefactorTool.getPlayerStats(victim).setDeaths();
            return;
        }

        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        Ref<EntityStore> attackerRef = entitySource.getRef();

        Player killer = store.getComponent(attackerRef, Player.getComponentType());

        if (killer != null) {
            if (willKillTarget(victimRef, damage.getAmount(), store)) { onPlayerKilledPlayer(killer, victim, damage); }
        } else {
            if (willKillTarget(victimRef, damage.getAmount(), store)) { onEntityKilledPlayer(victim, damage); }
        }
    }

    private boolean willKillTarget(Ref<EntityStore> targetRef, float damageAmount, Store<EntityStore> store) {
        EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (statMap == null) return false;

        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = statMap.get(healthIndex);
        assert health != null;
        float currentHealth = health.get();

        return currentHealth <= damageAmount;
    }

    private void onPlayerKilledPlayer(Player killer, Player victim, Damage damage) {
        Objects.requireNonNull(RefactorTool.getPlayerStats(killer)).setKills();
        Objects.requireNonNull(RefactorTool.getPlayerStats(victim)).setDeaths();

        Objects.requireNonNull(RefactorTool.getPlayerStats(killer)).setScore((int)damage.getAmount());
    }

    private void onEntityKilledPlayer(Player victim, Damage damage) { RefactorTool.getPlayerStats(victim).setDeaths(); }
}