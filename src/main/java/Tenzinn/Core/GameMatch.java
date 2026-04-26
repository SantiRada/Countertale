package Tenzinn.Core;

import Tenzinn.Core.Instances.InstanceManager;
import Tenzinn.Deathmatch.Flow.MatchDeathmatch;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;
import java.util.stream.Collectors;

public class GameMatch {

    private static final int MAX_PLAYERS = 10;

    public String mode;
    private String mapId;

    private List<String> eligibleMaps = new ArrayList<>();

    private final UUID matchId;
    private final List<PlayerRef> players;
    private MatchState state;
    private InstanceManager matchInstance;

    // Party group tracking for team formation
    private final List<List<PlayerRef>> partyGroups   = new ArrayList<>();
    private final Map<UUID, Integer>    teamAssignments = new HashMap<>();

    public enum MatchState { WAITING, STARTING, ON_PURCHASE, IN_PROGRESS, FINISHED }

    public GameMatch() {
        this.matchId = UUID.randomUUID();
        this.players = new ArrayList<>();
        this.state   = MatchState.WAITING;
    }
    public void addPlayer(PlayerRef playerRef) {
        if (players.size() >= MAX_PLAYERS) return;
        if (state != MatchState.WAITING) return;
        players.add(playerRef);
    }
    public void removePlayer(PlayerRef playerRef) { players.remove(playerRef); }
    public void initEligibleMaps(List<String> maps) {
        this.eligibleMaps = new ArrayList<>(maps);
    }
    public void intersectEligibleMaps(List<String> playerMaps) { eligibleMaps.retainAll(playerMaps); }
    public List<String> getEligibleMaps() {
        return Collections.unmodifiableList(eligibleMaps);
    }
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
    public void setInstance(InstanceManager instance) { matchInstance = instance; }
    public void removeInstance() { if (matchInstance != null) { matchInstance.removeInstance(); } }
    public String getMapId() { return mapId; }
    public void setMapId(String m){ this.mapId = m; }
    public void setState(MatchState state) { this.state = state; }
    public void setMode(String value) { mode = value; }
    public List<PlayerRef> getPlayers() { return new ArrayList<>(players); }
    public InstanceManager getInstance() { return matchInstance; }
    public int getPlayerCount() { return players.size(); }
    public MatchState getState() { return state; }
    public UUID getMatchId() { return matchId; }
    public String getMode() { return mode; }
    public boolean isBuyPhase() { return state == MatchState.STARTING || state == MatchState.ON_PURCHASE; }
    public boolean isFull() { return players.size() >= MAX_PLAYERS; }
    public boolean isEmpty() { return players.isEmpty(); }

    // ── Team formation ─────────────────────────────────────────────────────────
    public void addPartyGroup(List<PlayerRef> group) { partyGroups.add(new ArrayList<>(group)); }

    public Map<UUID, Integer> getTeamAssignments() { return teamAssignments; }

    /**
     * Assigns players to Team 1 (indices 0-4 in spawn list) or Team 2 (indices 5-9).
     * Keeps party members together in the same team using a greedy algorithm.
     * Called once per match at the start of the purchase phase.
     */
    public void formTeams() {
        if (!teamAssignments.isEmpty()) return;

        Set<UUID> activePlayers = players.stream()
                .map(PlayerRef::getUuid)
                .collect(Collectors.toSet());

        // Filter stale entries (players who left queue before match start)
        List<List<PlayerRef>> groups = partyGroups.stream()
                .map(g -> g.stream()
                        .filter(p -> activePlayers.contains(p.getUuid()))
                        .collect(Collectors.toList()))
                .filter(g -> !g.isEmpty())
                .sorted((a, b) -> b.size() - a.size()) // largest groups first
                .collect(Collectors.toList());

        List<PlayerRef> team1 = new ArrayList<>();
        List<PlayerRef> team2 = new ArrayList<>();

        for (List<PlayerRef> group : groups) {
            boolean fits1 = team1.size() + group.size() <= 5;
            boolean fits2 = team2.size() + group.size() <= 5;
            // Prefer the smaller team; if tied, prefer team 1
            if (fits1 && (!fits2 || team1.size() <= team2.size())) { team1.addAll(group); }
            else if (fits2)                                          { team2.addAll(group); }
            else if (team1.size() <= team2.size())                   { team1.addAll(group); } // overflow fallback
            else                                                     { team2.addAll(group); }
        }

        // Safety fallback: assign any ungrouped players to balance teams
        for (PlayerRef p : players) {
            if (!teamAssignments.containsKey(p.getUuid()) && !team1.contains(p) && !team2.contains(p)) {
                if (team1.size() < 5) { team1.add(p); }
                else                  { team2.add(p); }
            }
        }

        for (PlayerRef p : team1) teamAssignments.put(p.getUuid(), 1);
        for (PlayerRef p : team2) teamAssignments.put(p.getUuid(), 2);
    }
}
