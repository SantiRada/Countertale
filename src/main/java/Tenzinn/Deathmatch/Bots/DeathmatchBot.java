package Tenzinn.Deathmatch.Bots;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class DeathmatchBot {
    public final UUID matchId;
    public Ref<EntityStore> entityRef;
    public final String roleId;
    public final String displayName;
    public final String weaponId;
    public final String weaponDisplayName;

    public boolean alive;
    public int kills = 0;
    public int deaths = 0;
    public int score = 0;
    public long nextAttackAtMs = 0L;
    public long respawnAtMs = 0L;
    public long nextMovementAtMs = 0L;
    public long nextRepositionAtMs = 0L;
    public UUID targetPlayerUuid;
    public long targetLockUntilMs = 0L;
    public Vector3d lastKnownSpawn;
    public Vector3d lastTargetPosition;
    public Vector3d lastMovementObservedPosition;
    public long lastMovementObservedAtMs = 0L;
    public int failedMovementAttempts = 0;

    public DeathmatchBot(UUID matchId,
                         Ref<EntityStore> entityRef,
                         String roleId,
                         String displayName,
                         String weaponId,
                         String weaponDisplayName,
                         Vector3d spawnPosition) {
        this.matchId = matchId;
        this.entityRef = entityRef;
        this.roleId = roleId;
        this.displayName = displayName;
        this.weaponId = weaponId;
        this.weaponDisplayName = weaponDisplayName;
        this.alive = true;
        this.lastKnownSpawn = spawnPosition != null ? new Vector3d(spawnPosition) : null;
    }
}
