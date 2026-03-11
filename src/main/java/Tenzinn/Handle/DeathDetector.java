package Tenzinn.Handle;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.LootManager;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

import java.awt.*;
import java.util.ArrayList;
import javax.annotation.Nonnull;

public class DeathDetector extends DeathSystems.OnDeathSystem {

    @Nonnull @Override
    public Query<EntityStore> getQuery() { return Query.and(Player.getComponentType()); }

    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {

        Damage deathInfo = component.getDeathInfo();
        Player victim = (Player) store.getComponent(ref, Player.getComponentType());
        if (victim == null) return;

        if (victim.getWorld() != Universe.get().getDefaultWorld()) {
            String causeId = component.getDeathCause() != null ? component.getDeathCause().getId() : "unknown";
            float damage = deathInfo.getInitialAmount();

            if (deathInfo != null && deathInfo.getSource() instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> killerRef = entitySource.getRef();
                Player killer = (Player) store.getComponent(killerRef, Player.getComponentType());


                if (killer != null) {
                    trackAttackerDamage(killer, damage);

                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.KILL, 0);
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.SCORE, damage);
                }
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
            else if (deathInfo != null && deathInfo.getSource() instanceof Damage.ProjectileSource projectileSource) {
                Ref<EntityStore> shooterRef = projectileSource.getRef();
                Player killer = (Player) store.getComponent(shooterRef, Player.getComponentType());

                if (killer != null) {
                    trackAttackerDamage(killer, damage);
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.KILL, 0);
                    RefactorTool.setDataScore(killer, RefactorTool.TypeData.SCORE, damage);
                }
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
            else {
                // Caída, void, /kill, comando, o cualquier source anónimo
                System.out.println("[DeathDetector] Muerte por entorno/comando, causa: " + causeId);
                RefactorTool.setDataScore(victim, RefactorTool.TypeData.DEATH, 0);
            }
        }
    }

    private void trackAttackerDamage(Player attacker, float damage) {
        var item = attacker.getInventory().getActiveHotbarItem();
        boolean isMelee = false;
        if (item != null && item.getItemId() != null) {
            String itemId = item.getItemId();
            isMelee = itemId.contains("knife")
                    || itemId.contains("daggers")
                    || itemId.contains("sword");
        }
        RefactorTool.setDamageCaused(attacker, damage);
        if (isMelee) RefactorTool.setMeleeDamage(attacker, damage);
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(playerComponent.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;

        ArrayList<WeaponStats> loot = LootManager.getGameLoot(playerComponent);
        if (loot != null) LootManager.giveLoot(playerComponent, loot);

        if (playerComponent.getWorld() != Universe.get().getDefaultWorld()) { RefactorTool.Respawn(playerRef); }
    }
}