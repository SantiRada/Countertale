package Tenzinn.Deathmatch;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.Instances.InstanceManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    public enum MatchState { WAITING, STARTING, IN_PROGRESS, FINISHED }

    public GameMatch() {
        this.matchId = UUID.randomUUID();
        this.players = new ArrayList<>();
        this.state = MatchState.WAITING;
    }
    public boolean addPlayer(PlayerRef playerRef) {
        if (players.size() >= MAX_PLAYERS) return false;
        if (state != MatchState.WAITING) return false;

        players.add(playerRef);

        return true;
    }
    public void setInstance(Countertale main) { matchInstance = new InstanceManager(main); }
    public InstanceManager getInstance() { return matchInstance; }
    public boolean removePlayer(PlayerRef playerRef) { return players.remove(playerRef); }
    public void removeInstance() { if (matchInstance != null) { matchInstance.removeInstance(); } }
    public boolean isFull() { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty() { return players.isEmpty(); }
    public int getPlayerCount() { return players.size(); }
    public List<PlayerRef> getPlayers() { return new ArrayList<>(players); }
    public UUID getMatchId() { return matchId; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
}