package Tenzinn.Deathmatch.UI;

import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ScoreboardPage extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerRef playerRef;

    public ScheduledFuture<?> timerTask;

    public ScoreboardPage(PlayerRef playerRef) { super(playerRef, playerRef.getUuid().toString()); this.playerRef = playerRef; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/DM/Scoreboard_DM.ui");
        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();
    }
    public void setData() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
        int index = 1;

        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for(PlayerStats player : playersList) {
            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(player.getPlayerRef().getUsername()));
            uiBuilder.set("#Kill0" + index + ".TextSpans", Message.raw(String.valueOf(player.getKills())));
            uiBuilder.set("#Death0" + index + ".TextSpans", Message.raw(String.valueOf(player.getDeaths())));
            uiBuilder.set("#Score0" + index + ".TextSpans", Message.raw(String.valueOf(player.getScore())));

            if(player.getPlayerRef().equals(playerRef)) {
                if (index == 1) { uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#27F5A3"); }

                uiBuilder.set("#DataUser0" + index + ".OutlineSize", 2);
            }

            index += 1;
        }

        // Fallback
        for (int i = index; i <= 10; i++) {
            uiBuilder.set("#Name0" + i + ".TextSpans", Message.raw(""));
            uiBuilder.set("#Kill0" + i + ".TextSpans", Message.raw(""));
            uiBuilder.set("#Death0" + i + ".TextSpans", Message.raw(""));
            uiBuilder.set("#Score0" + i + ".TextSpans", Message.raw(""));
        }

        String[] nameWorld = Objects.requireNonNull(RefactorTool.getPlayer(playerRef).getWorld()).getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }

        uiBuilder.set("#NameMap.TextSpans", Lang.msg("ui.scoreboard.casual-map", "map", worldNameWithSpaces));

        update(true, uiBuilder);
    }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            int remainingSeconds = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch().getTimer();

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            String timerText = String.format("%02d:%02d", minutes, seconds);

            uiBuilder.set("#Timer.TextSpans", Message.raw(timerText));

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
