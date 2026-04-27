package Tenzinn.Core.Handle;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Set;
import javax.annotation.Nonnull;

public class DeathDetector extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player victim = (Player) store.getComponent(ref, Player.getComponentType());
        if (victim == null) return;

        // Only handle custom deaths inside match worlds.
        if (victim.getWorld() == Universe.get().getDefaultWorld()) {
            return;
        }

        component.setShowDeathMenu(false);

        PlayerRef playerRef = Universe.get().getPlayerByUsername(victim.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats == null || playerStats.getCurrentMatch() == null) return;

        Damage deathInfo = component.getDeathInfo();
        String causeId = component.getDeathCause() != null ? component.getDeathCause().getId() : "unknown";

        applyDeathStats(victim, deathInfo, store, causeId);

        String mode = playerStats.getCurrentMatch().getMode();

        if (mode.equalsIgnoreCase("dm")) {
            DeathComponent.respawn(store, ref);
            return;
        }

        if (mode.equalsIgnoreCase("fvf")) {
            playerStats.playerState = PlayerStats.PlayerState.SPECTATOR;
            MatchFVF.validateFinishRound();
        }
    }

    private void applyDeathStats(Player victim,
                                 Damage deathInfo,
                                 Store<EntityStore> store,
                                 String causeId) {
        float damage = 0;
        Player killer = null;

        if (deathInfo != null) {
            damage = deathInfo.getInitialAmount();

            if (deathInfo.getSource() instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> killerRef = entitySource.getRef();
                if (killerRef != null) {
                    killer = (Player) store.getComponent(killerRef, Player.getComponentType());
                }
            } else if (deathInfo.getSource() instanceof Damage.ProjectileSource projectileSource) {
                Ref<EntityStore> shooterRef = projectileSource.getRef();
                if (shooterRef != null) {
                    killer = (Player) store.getComponent(shooterRef, Player.getComponentType());
                }
            } else {
                System.out.println("[DeathDetector] Death by environment/command, cause: " + causeId);
            }
        } else {
            System.out.println("[DeathDetector] Death had no deathInfo, cause: " + causeId);
        }

        GameMatch matchToRefresh = null;

        if (killer != null && !killer.getDisplayName().equals(victim.getDisplayName())) {
            PlayerRef killerPlayerRef = Universe.get().getPlayerByUsername(killer.getDisplayName(), NameMatching.EXACT);

            if (killerPlayerRef != null) {
                PlayerStats killerStats = RefactorTool.getPlayerStats(killerPlayerRef);

                if (killerStats != null) {
                    killerStats.setKills();
                    killerStats.setScore((int) damage);
                    matchToRefresh = killerStats.getCurrentMatch();
                }
            }
        }

        PlayerRef victimPlayerRef = Universe.get().getPlayerByUsername(victim.getDisplayName(), NameMatching.EXACT);

        if (victimPlayerRef != null) {
            PlayerStats victimStats = RefactorTool.getPlayerStats(victimPlayerRef);

            if (victimStats != null) {
                victimStats.setDeaths();

                if (matchToRefresh == null) {
                    matchToRefresh = victimStats.getCurrentMatch();
                }
            }
        }

        if (matchToRefresh != null) {
            RefactorTool.setChangesInUI(matchToRefresh);
        }
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull DeathComponent component,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(playerComponent.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats == null || playerStats.getCurrentMatch() == null) return;

        String mode = playerStats.getCurrentMatch().getMode();

        if (mode.equalsIgnoreCase("dm")) {
            ArrayList<WeaponStats> loot = LootManager.getGameLoot(playerComponent);
            if (loot != null) {
                LootManager.giveLoot(playerComponent, loot);
            }

            if (playerComponent.getWorld() != Universe.get().getDefaultWorld()) {
                RefactorTool.Respawn(playerRef);
            }

            return;
        }

        if (mode.equalsIgnoreCase("fvf")) {
            MatchFVF.validateFinishRound();
        }
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.PlayerDeathScreen.class));
    }
}