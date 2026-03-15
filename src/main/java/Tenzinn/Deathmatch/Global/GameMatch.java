package Tenzinn.Deathmatch.Global;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.Content.Effects.StaminaInfinite;
import Tenzinn.Deathmatch.Global.Tools.RefactorTool;
import com.hypixel.hytale.server.core.HytaleServer;
import Tenzinn.Deathmatch.Content.Instances.InstanceManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    public String mode;

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    public enum MatchState { WAITING, STARTING, IN_PROGRESS, FINISHED }

    // In-Game Content
    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600; // 10:00

    public GameMatch() {
        this.matchId = UUID.randomUUID();
        this.players = new ArrayList<>();
        this.state = MatchState.WAITING;
    }
    public void addPlayer(PlayerRef playerRef) {
        if (players.size() >= MAX_PLAYERS) return;
        if (state != MatchState.WAITING) return;

        players.add(playerRef);
    }
    // ================================================ //
    public void activateEffects() {
        ArrayList<PlayerRef> playersList = new ArrayList<>(players);
        StaminaInfinite.apply(playersList);
    }
    // ================================================ //
    public void startTimer() {
        if (timerTask != null && !timerTask.isDone()) return;
        
        if (state == MatchState.STARTING) {
            remainingSeconds = 15;
            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0) {
                    stopTimer();

                    setState(MatchState.IN_PROGRESS);
                    startTimer();

                    return;
                }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
        else {
            remainingSeconds = 600;

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0) {
                    World world = Universe.get().getWorld(Objects.requireNonNull(players.getFirst().getWorldUuid()));
                    assert world != null;
                    world.execute(() -> { RefactorTool.finishGame(players); });
                }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }
    public int getTimer () { return remainingSeconds; }
    public void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }
    // ================================================ //
    public void setInstance(Countertale main) { matchInstance = new InstanceManager(main); }
    public void setInstance(InstanceManager instance) { matchInstance = instance; }
    public void setState(MatchState state) { this.state = state; }
    public void setMode(String value) { mode = value; }
    // ================================================ //
    public boolean isBuyPhase () { return state == MatchState.STARTING; }
    // ================================================ //
    public List<PlayerRef> getPlayers() { return new ArrayList<>(players); }
    public InstanceManager getInstance() { return matchInstance; }
    public int getPlayerCount() { return players.size(); }
    public MatchState getState() { return state; }
    public UUID getMatchId() { return matchId; }
    public String getMode() { return mode; }
    // ================================================ //
    public void removeInstance() { if (matchInstance != null) { matchInstance.removeInstance(); } }
    public void removePlayer(PlayerRef playerRef) {
        StaminaInfinite.remove(playerRef);
        players.remove(playerRef);
    }
    // ================================================ //
    public boolean isFull() { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty() { return players.isEmpty(); }
}