package Tenzinn.Core.Events;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.GameHUD;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHealthTracker extends DelayedEntitySystem<EntityStore> {

    private static final Map<UUID, Float> currentHealthMap    = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> maxHealthMap        = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastUpdatedHealthMap = new ConcurrentHashMap<>();

    public PlayerHealthTracker() { super(0.2f); }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComp == null) return;
        UUID uuid = uuidComp.getUuid();

        EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) return;

        EntityStatValue healthStat = statMap.get(DefaultEntityStatTypes.getHealth());
        if (healthStat == null) return;

        float current = healthStat.get();

        float lastUpdated = lastUpdatedHealthMap.getOrDefault(uuid, -1f);
        if (current == lastUpdated) return;

        float max = healthStat.getMax();

        currentHealthMap.put(uuid, current);
        maxHealthMap.put(uuid, max);
        lastUpdatedHealthMap.put(uuid, current);

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        CustomUIHud customHUD = player.getHudManager().getCustomHud();
        if (!(customHUD instanceof GameHUD newHud)) return;

        newHud.setHealth((int)current, (int)max);

        if (player.getWorld() == null) return;
        World world = Universe.get().getWorld(player.getWorld().getWorldConfig().getUuid());
        if (world == Universe.get().getDefaultWorld()) return;

        float newAmount = RefactorTool.getMaxHealth(player) - current;
        RefactorTool.setDamageReceived(player, newAmount);
        RefactorTool.setHealthPlayer(player, current);
    }

    @Nullable @Override
    public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getInspectDamageGroup(); }

    @Nonnull @Override
    public Query<EntityStore> getQuery() { return Query.and(Player.getComponentType()); }

    public static float getCurrentHealth(UUID uuid) { return currentHealthMap.getOrDefault(uuid, 0f); }
}