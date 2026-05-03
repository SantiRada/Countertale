package Tenzinn.FiveVSfive.UI;

import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ScoreboardPageFVF extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerRef playerRef;

    public ScheduledFuture<?> timerTask;

    public ScoreboardPageFVF(PlayerRef playerRef) { super(playerRef); this.playerRef = playerRef; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/FVF/Scoreboard_FVF.ui");
        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();
    }

    public void setData() {
        PlayerStats myStats = RefactorTool.getPlayerStats(playerRef);
        if (myStats == null) return;

        List<PlayerStats> all = RefactorTool.getPlayerList(myStats.getCurrentMatch());

        List<PlayerStats> team1 = new ArrayList<>();
        List<PlayerStats> team2 = new ArrayList<>();
        for (PlayerStats ps : all) {
            int team = MatchFVF.validateTeamMembership(ps.getPlayerRef());
            if (team == 1) team1.add(ps);
            else           team2.add(ps);
        }

        team1.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));
        team2.sort((a, b) -> Integer.compare(b.getKills(), a.getKills()));

        fillTeam(team1, "T1");
        fillTeam(team2, "T2");

        int r1 = MatchFVF.getNumberRound(1);
        int r2 = MatchFVF.getNumberRound(2);
        uiBuilder.set("#Team1Rounds.TextSpans", Message.raw(r1 + (r1 == 1 ? " round" : " rounds")));
        uiBuilder.set("#Team2Rounds.TextSpans", Message.raw(r2 + (r2 == 1 ? " round" : " rounds")));

        String[] nameWorld = RefactorTool.getPlayer(playerRef).getWorld().getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }
        uiBuilder.set("#NameMap.TextSpans", Lang.msg("ui.scoreboard.competitive-map", "map", worldNameWithSpaces));

        update(true, uiBuilder);
    }

    private void fillTeam(List<PlayerStats> team, String prefix) {
        for (int i = 0; i < 5; i++) {
            String slot = String.format("%02d", i + 1);
            if (i < team.size()) {
                PlayerStats ps = team.get(i);
                uiBuilder.set("#" + prefix + "Name" + slot + ".TextSpans",  Message.raw(ps.getPlayer().getDisplayName()));
                uiBuilder.set("#" + prefix + "Kill" + slot + ".TextSpans",  Message.raw(String.valueOf(ps.getKills())));
                uiBuilder.set("#" + prefix + "Death" + slot + ".TextSpans", Message.raw(String.valueOf(ps.getDeaths())));
                uiBuilder.set("#" + prefix + "Score" + slot + ".TextSpans", Message.raw(String.valueOf(ps.getScore())));

                if (ps.getPlayerRef().equals(playerRef)) {
                    uiBuilder.set("#" + prefix + "DataUser" + slot + ".OutlineColor", "#27F5A3");
                    uiBuilder.set("#" + prefix + "DataUser" + slot + ".OutlineSize", 2);
                } else {
                    uiBuilder.set("#" + prefix + "DataUser" + slot + ".OutlineSize", 0);
                }
            } else {
                uiBuilder.set("#" + prefix + "Name" + slot + ".TextSpans",  Message.raw(""));
                uiBuilder.set("#" + prefix + "Kill" + slot + ".TextSpans",  Message.raw(""));
                uiBuilder.set("#" + prefix + "Death" + slot + ".TextSpans", Message.raw(""));
                uiBuilder.set("#" + prefix + "Score" + slot + ".TextSpans", Message.raw(""));
                uiBuilder.set("#" + prefix + "DataUser" + slot + ".OutlineSize", 0);
            }
        }
    }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            int remainingSeconds = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch().getTimer();

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            uiBuilder.set("#Timer.TextSpans", Message.raw(String.format("%02d:%02d", minutes, seconds)));

            update(true, uiBuilder);
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void clearHUD() {
        if (timerTask != null && !timerTask.isDone()) timerTask.cancel(true);

        if (uiBuilder == null) return;

        try {
            uiBuilder.remove("#Scoreboard");
            update(true, uiBuilder);
        } catch (Exception e) { }

        uiBuilder = null;
    }
}
