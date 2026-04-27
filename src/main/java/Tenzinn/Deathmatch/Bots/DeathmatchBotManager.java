package Tenzinn.Deathmatch.Bots;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathmatchBotManager {

    private static final int MAX_DM_PARTICIPANTS = 10;

    private static final Map<UUID, List<DeathmatchBot>> BOTS_BY_MATCH = new ConcurrentHashMap<>();

    public static void spawnFillBots(GameMatch match, World world, String mapId) {
        if (match == null || world == null || mapId == null) return;
        if (!match.getMode().equalsIgnoreCase("dm")) return;

        int humans = match.getPlayerCount();
        int botsNeeded = Math.max(0, MAX_DM_PARTICIPANTS - humans);

        if (botsNeeded <= 0) return;

        ArrayList<Vector3d> spawns = RefactorTool.getSpawns(mapId, MapListeners.SpawnMode.DM);
        if (spawns == null || spawns.isEmpty()) return;

        BOTS_BY_MATCH.computeIfAbsent(match.getMatchId(), id -> new ArrayList<>());

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();

            for (int i = 0; i < botsNeeded; i++) {
                String roleId = (i % 2 == 0) ? "T_DefaultAgent" : "CT_DefaultAgent";
                String name = (i % 2 == 0) ? "T Bot " + (i + 1) : "CT Bot " + (i + 1);

                Vector3d spawn = spawns.get((humans + i) % spawns.size());
                Vector3f rotation = new Vector3f(0f, 0f, 0f);

                /*
                 * Spawn call goes here:
                 *
                 * var result = NPCPlugin.get().spawnNPC(store, roleId, null, spawn, rotation);
                 *
                 * The exact imports may differ depending on the current Hytale server jar,
                 * but this is the API shape shown in the current community plugin docs.
                 */

                // After spawn succeeds:
                // Ref<EntityStore> botRef = result.first();
                // BOTS_BY_MATCH.get(match.getMatchId()).add(new DeathmatchBot(match.getMatchId(), botRef, roleId, name));
            }
        });
    }

    public static List<DeathmatchBot> getBots(GameMatch match) {
        if (match == null) return List.of();
        return BOTS_BY_MATCH.getOrDefault(match.getMatchId(), List.of());
    }

    public static void removeBots(GameMatch match) {
        if (match == null) return;

        List<DeathmatchBot> bots = BOTS_BY_MATCH.remove(match.getMatchId());
        if (bots == null || bots.isEmpty()) return;

        for (DeathmatchBot bot : bots) {
            try {
                if (bot.entityRef != null && bot.entityRef.isValid()) {
                    Store<EntityStore> store = bot.entityRef.getStore();
                    store.removeEntity(bot.entityRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
                }
            } catch (Exception ignored) {
            }
        }
    }
}