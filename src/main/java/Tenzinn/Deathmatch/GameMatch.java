package Tenzinn.Deathmatch;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.HytaleServer;
import Tenzinn.Deathmatch.Instances.InstanceManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    public enum MatchState { WAITING, STARTING, IN_PROGRESS, FINISHED }

    // In-Game Content
    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600; // 10:00
    private String timerText = "";

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
    // ================================================ //
    public void startTimer() {
        remainingSeconds = 600;

        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            if (remainingSeconds <= 0) {
                stopTimer();
                return;
            }

            remainingSeconds--;

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timerText = String.format("%02d:%02d", minutes, seconds);
        }, 1, 1, TimeUnit.SECONDS);
    }
    public int getTimer () { return remainingSeconds; }

    public void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }

    // ================================================ //
    public void setInstance(Countertale main) { matchInstance = new InstanceManager(main); }
    public void setInstance(InstanceManager instance) { matchInstance = instance; }
    public void setState(MatchState state) { this.state = state; }
    // ================================================ //
    public List<PlayerRef> getPlayers() { return new ArrayList<>(players); }
    public InstanceManager getInstance() { return matchInstance; }
    public int getPlayerCount() { return players.size(); }
    public MatchState getState() { return state; }
    public UUID getMatchId() { return matchId; }
    // ================================================ //
    public void removeInstance() { if (matchInstance != null) { matchInstance.removeInstance(); } }
    public void removePlayer(PlayerRef playerRef) { players.remove(playerRef); }
    // ================================================ //
    public boolean isFull() { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty() { return players.isEmpty(); }
}