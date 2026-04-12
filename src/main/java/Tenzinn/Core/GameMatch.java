package Tenzinn.Core;

import Tenzinn.Core.Instances.InstanceManager;
import Tenzinn.Deathmatch.Flow.MatchDeathmatch;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    public String mode;
    private String mapId;

    /**
     * Intersección de los mapas votados por todos los jugadores en esta partida.
     * Se establece con el primer jugador y se va reduciendo a medida que se unen más.
     * Cuando la partida inicia, el mapa final se elige de este conjunto.
     */
    private List<String> eligibleMaps = new ArrayList<>();

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    public enum MatchState { WAITING, STARTING, ON_PURCHASE, IN_PROGRESS, FINISHED }

    public GameMatch() {
        this.matchId = UUID.randomUUID();
        this.players = new ArrayList<>();
        this.state   = MatchState.WAITING;
    }

    // ── Jugadores ─────────────────────────────────────────────────────────────

    public void addPlayer(PlayerRef playerRef) {
        if (players.size() >= MAX_PLAYERS) return;
        if (state != MatchState.WAITING) return;
        players.add(playerRef);
    }

    public void removePlayer(PlayerRef playerRef) { players.remove(playerRef); }

    // ── Mapas elegibles (sistema de votos) ───────────────────────────────────

    /**
     * Establece los mapas elegibles cuando se crea la partida con el primer jugador.
     */
    public void initEligibleMaps(List<String> maps) {
        this.eligibleMaps = new ArrayList<>(maps);
    }

    /**
     * Reduce los mapas elegibles a la intersección con los del nuevo jugador.
     *
     * @param playerMaps mapas aceptados por el jugador que se une
     * @return true si la intersección es no vacía (jugador compatible), false si es vacía
     */
    public boolean intersectEligibleMaps(List<String> playerMaps) {
        eligibleMaps.retainAll(playerMaps);
        return !eligibleMaps.isEmpty();
    }

    /** Devuelve la lista actual de mapas elegibles (inmutable). */
    public List<String> getEligibleMaps() {
        return Collections.unmodifiableList(eligibleMaps);
    }

    // ── Timer y flujo de partida ──────────────────────────────────────────────

    public void startTimer() {
        if (mode.equalsIgnoreCase("dm")) {
            MatchDeathmatch.startTimerMatch(this);
        } else {
            players.getFirst().sendMessage(Message.raw("Cargando timer de 5v5"));
            MatchFVF.startTimerMatch(this);
        }
    }

    public int getTimer() {
        if (mode.equalsIgnoreCase("dm")) { return MatchDeathmatch.getTimer(); }
        else { return MatchFVF.getTimer(); }
    }

    public void stopTimer() {
        if (mode.equalsIgnoreCase("dm")) { MatchDeathmatch.stopTimer(); }
        else { MatchFVF.stopTimer(); }
    }

    // ── Instancia ─────────────────────────────────────────────────────────────

    public void setInstance(InstanceManager instance) { matchInstance = instance; }

    public void removeInstance() {
        if (matchInstance != null) { matchInstance.removeInstance(); }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getMapId()        { return mapId; }
    public void   setMapId(String m){ this.mapId = m; }

    public void        setState(MatchState state) { this.state = state; }
    public void        setMode(String value)       { mode = value; }

    public List<PlayerRef>  getPlayers()     { return new ArrayList<>(players); }
    public InstanceManager  getInstance()    { return matchInstance; }
    public int              getPlayerCount() { return players.size(); }
    public MatchState       getState()       { return state; }
    public UUID             getMatchId()     { return matchId; }
    public String           getMode()        { return mode; }

    public boolean isBuyPhase() { return state == MatchState.STARTING || state == MatchState.ON_PURCHASE; }
    public boolean isFull()     { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty()    { return players.isEmpty(); }
}
