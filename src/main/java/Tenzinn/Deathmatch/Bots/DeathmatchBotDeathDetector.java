package Tenzinn.Deathmatch.Bots;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.Set;

public class DeathmatchBotDeathDetector extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!DeathmatchBotManager.isBot(ref)) {
            return;
        }

        component.setShowDeathMenu(false);

        Ref<EntityStore> killerRef = getKillerRef(component.getDeathInfo());
        DeathmatchBotManager.handleBotDeath(ref, killerRef);
    }

    private Ref<EntityStore> getKillerRef(Damage deathInfo) {
        if (deathInfo == null || deathInfo.getSource() == null) {
            return null;
        }

        if (deathInfo.getSource() instanceof Damage.EntitySource entitySource) {
            return entitySource.getRef();
        }
        if (deathInfo.getSource() instanceof Damage.ProjectileSource projectileSource) {
            return projectileSource.getRef();
        }

        return null;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, DeathSystems.PlayerDeathScreen.class));
    }
}
