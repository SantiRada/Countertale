package Tenzinn.Core.Handle;

import Tenzinn.Core.LootManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Listeners.MapListeners;

import Tenzinn.Core.UI.DiePage;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;

import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nonnull;

public class DeathDetector extends DeathSystems.OnDeathSystem {

    @Nonnull @Override
    public Query<EntityStore> getQuery() { return Query.and(Player.getComponentType()); }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Damage deathInfo = component.getDeathInfo();
        Player victim = (Player) store.getComponent(ref, Player.getComponentType());
        if (victim == null) return;

        if (victim.getWorld() != Universe.get().getDefaultWorld()) {
            String causeId = component.getDeathCause() != null ? component.getDeathCause().getId() : "unknown";
            assert deathInfo != null;
            float damage = deathInfo.getInitialAmount();

            if (deathInfo != null && deathInfo.getSource() instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> killerRef = entitySource.getRef();
                Player killer = (Player) store.getComponent(killerRef, Player.getComponentType());

                if (killer != null) {
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.KILL, 0);
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.SCORE, damage);
                }
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
            else if (deathInfo.getSource() instanceof Damage.ProjectileSource projectileSource) {
                Ref<EntityStore> shooterRef = projectileSource.getRef();
                Player killer = (Player) store.getComponent(shooterRef, Player.getComponentType());

                if (killer != null) {
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.KILL, 0);
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.SCORE, damage);
                }
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
            else {
                // Fall, void, /kill, command, or any anonymous source
                System.out.println("[DeathDetector] Death by environment/command, cause: " + causeId);
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
        }

        // RespawnPage disabled
        component.setShowDeathMenu(false);
        PlayerRef playerRef = Universe.get().getPlayerByUsername(victim.getDisplayName(), NameMatching.EXACT);

        // In FvF, mark the player as SPECTATOR so validateFinishRound() counts them as dead.
        // The round system (resetPlayers) will revive them — no manual respawn allowed.
        if (victim.getWorld() != Universe.get().getDefaultWorld()) {
            var ps = RefactorTool.getPlayerStats(playerRef);
            if (ps != null && RefactorTool.getModeForPlayer(playerRef) == MapListeners.SpawnMode.FVF) {
                ps.playerState = Tenzinn.Core.Objects.PlayerStats.PlayerState.SPECTATOR;
            }
        }
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(playerComponent.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;

        if (RefactorTool.getModeForPlayer(playerRef) == MapListeners.SpawnMode.DM) {
            ArrayList<WeaponStats> loot = LootManager.getGameLoot(playerComponent);
            if (loot != null) LootManager.giveLoot(playerComponent, loot);

            if (playerComponent.getWorld() != Universe.get().getDefaultWorld()) { RefactorTool.Respawn(playerRef); }
        } else {
            // Spectator Mode Pending
            if(MatchFVF.validateFinishRound() <= 0) return;

            ArrayList<WeaponStats> loot = LootManager.getStarterKit();
            LootManager.giveLoot(playerComponent, loot);
        }
    }

    @Nonnull @Override
    public Set<Dependency<EntityStore>> getDependencies() { return Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.PlayerDeathScreen.class)); }
}
