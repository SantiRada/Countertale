package Tenzinn.Deathmatch.Bots;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class DeathmatchBot {
    public final UUID matchId;
    public final Ref<EntityStore> entityRef;
    public final String roleId;
    public final String displayName;

    public int kills = 0;
    public int deaths = 0;
    public int score = 0;

    public DeathmatchBot(UUID matchId, Ref<EntityStore> entityRef, String roleId, String displayName) {
        this.matchId = matchId;
        this.entityRef = entityRef;
        this.roleId = roleId;
        this.displayName = displayName;
    }
}