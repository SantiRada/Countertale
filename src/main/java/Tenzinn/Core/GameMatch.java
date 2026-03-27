package Tenzinn.Core;

import Tenzinn.Countertale;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import Tenzinn.Core.Instances.InstanceManager;
import Tenzinn.Deathmatch.Flow.MatchDeathmatch;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    public String mode;

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    public enum MatchState { WAITING, STARTING, ON_PURCHASE, IN_PROGRESS, FINISHED }

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
    public void startTimer() {
        if (mode.equalsIgnoreCase("dm")) {
            // Funcionamiento del temporizador de etapa de compra / tiempo de partida
            MatchDeathmatch.startTimerMatch(this);
        }
        else {
            // Funcionamiento del temporizador de etapa de compra / tiempo de ronda
            players.get(0).sendMessage(Message.raw("Cargando timer de 5v5"));
            MatchFVF.startTimerMatch(this);
        }
    }
    public int getTimer () {
        if(mode.equalsIgnoreCase("dm")) { return MatchDeathmatch.getTimer(); }
        else { return MatchFVF.getTimer(); }
    }
    public void stopTimer() {
        if(mode.equalsIgnoreCase("dm")) { MatchDeathmatch.stopTimer(); }
        else { MatchFVF.stopTimer(); }
    }
    // ================================================ //
    public void setInstance(Countertale main) { matchInstance = new InstanceManager(main); }
    public void setInstance(InstanceManager instance) { matchInstance = instance; }
    public void setState(MatchState state) { this.state = state; }
    public void setMode(String value) { mode = value; }
    // ================================================ //
    public boolean isBuyPhase () { return state == MatchState.STARTING || state == MatchState.ON_PURCHASE; }
    // ================================================ //
    public List<PlayerRef> getPlayers() { return new ArrayList<>(players); }
    public InstanceManager getInstance() { return matchInstance; }
    public int getPlayerCount() { return players.size(); }
    public MatchState getState() { return state; }
    public UUID getMatchId() { return matchId; }
    public String getMode() { return mode; }
    // ================================================ //
    public void removeInstance() { if (matchInstance != null) { matchInstance.removeInstance(); } }
    public void removePlayer(PlayerRef playerRef) { players.remove(playerRef); }
    // ================================================ //
    public boolean isFull() { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty() { return players.isEmpty(); }
}