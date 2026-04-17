package Tenzinn.Core.Instances;

import Tenzinn.Core.Listeners.MapListeners;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MapVoteStore {

    private static final Map<UUID, List<String>> votes = new ConcurrentHashMap<>();

    private MapVoteStore() {}
    public static void setVotes(PlayerRef playerRef, List<String> maps) {
        if (playerRef == null || maps == null) return;
        votes.put(playerRef.getUuid(), Collections.unmodifiableList(new ArrayList<>(maps)));
    }
    public static List<String> getVotes(PlayerRef playerRef) {
        if (playerRef == null) return new ArrayList<>(MapListeners.getMapNames());
        List<String> stored = votes.get(playerRef.getUuid());
        if (stored == null || stored.isEmpty()) {
            return new ArrayList<>(MapListeners.getMapNames());
        }
        return new ArrayList<>(stored);
    }
    public static void clearVotes(PlayerRef playerRef) {
        if (playerRef != null) votes.remove(playerRef.getUuid());
    }
}
