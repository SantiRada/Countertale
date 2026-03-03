package Tenzinn.Deathmatch.Effects;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier.ModifierTarget;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Objects;

public class StaminaInfinite {

    private static final String MODIFIER_KEY = "infinite_stamina";

    public static void apply(ArrayList<PlayerRef> players) {
        for (PlayerRef playerRef : players) {
            assert playerRef.getWorldUuid() != null;
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) continue;

            world.execute(() -> {
                Store<EntityStore> store = Objects.requireNonNull(playerRef.getReference()).getStore();

                EntityStatMap statMap = store.getComponent(Objects.requireNonNull(playerRef.getReference()), EntityStatsModule.get().getEntityStatMapComponentType());
                if (statMap == null) return;

                int staminaIndex = DefaultEntityStatTypes.getStamina();
                EntityStatValue staminaValue = statMap.get(staminaIndex);
                if (staminaValue == null) return;

                float maxStamina = staminaValue.getMax();

                StaticModifier infiniteMod = new StaticModifier(ModifierTarget.MIN, StaticModifier.CalculationType.ADDITIVE, maxStamina);

                statMap.putModifier(staminaIndex, MODIFIER_KEY, infiniteMod);

                statMap.maximizeStatValue(staminaIndex);
            });
        }
    }

    public static void remove(PlayerRef playerRef) {
        assert playerRef.getWorldUuid() != null;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        assert world != null;

        world.execute(() -> {
            Store<EntityStore> store = Objects.requireNonNull(playerRef.getReference()).getStore();

            EntityStatMap statMap = store.getComponent(playerRef.getReference(), EntityStatsModule.get().getEntityStatMapComponentType());
            if (statMap == null) return;

            int staminaIndex = DefaultEntityStatTypes.getStamina();

            statMap.removeModifier(staminaIndex, MODIFIER_KEY);
        });
    }
}