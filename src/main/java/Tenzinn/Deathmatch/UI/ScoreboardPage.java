package Tenzinn.Deathmatch.UI;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Objects.PlayerStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ScoreboardPage extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerRef playerRef;

    public ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600;

    public ScoreboardPage(PlayerRef playerRef) { super(playerRef); this.playerRef = playerRef; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Scoreboard.ui");
        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();
    }
    public void setData() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList();
        int index = 1;

        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for(PlayerStats player : playersList) {
            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(player.getName()));
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

        String[] nameWorld = RefactorTool.getPlayer(playerRef).getWorld().getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }

        uiBuilder.set("#NameMap.TextSpans", Message.raw("Casual | " + worldNameWithSpaces));

        update(true, uiBuilder);
    }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            remainingSeconds = RefactorTool.getPlayerStats(playerRef).getCurrentMatch().getTimer();

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