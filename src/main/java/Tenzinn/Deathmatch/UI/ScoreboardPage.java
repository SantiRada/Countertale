package Tenzinn.Deathmatch.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Deathmatch.Bots.DeathmatchBot;
import Tenzinn.Deathmatch.Bots.DeathmatchBotManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ScoreboardPage extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerRef playerRef;

    public ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600;

    public ScoreboardPage(PlayerRef playerRef) { super(playerRef); this.playerRef = playerRef; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/DM/Scoreboard_DM.ui");
        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();
    }

    public void setData() {
        if (uiBuilder == null) return;

        PlayerStats viewerStats = RefactorTool.getPlayerStats(playerRef);
        if (viewerStats == null || viewerStats.getCurrentMatch() == null) return;

        GameMatch match = viewerStats.getCurrentMatch();
        List<ScoreEntry> rows = buildRows(match);
        int index = 1;

        for (ScoreEntry row : rows) {
            if (index > 10) break;

            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(row.name()));
            uiBuilder.set("#Kill0" + index + ".TextSpans", Message.raw(String.valueOf(row.kills())));
            uiBuilder.set("#Death0" + index + ".TextSpans", Message.raw(String.valueOf(row.deaths())));
            uiBuilder.set("#Score0" + index + ".TextSpans", Message.raw(String.valueOf(row.score())));
            uiBuilder.set("#DataUser0" + index + ".OutlineSize", 0);
            uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#E0B448");

            PlayerRef rowPlayerRef = row.playerRef();
            if (rowPlayerRef != null && rowPlayerRef.equals(playerRef)) {
                if (index == 1) {
                    uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#27F5A3");
                }

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
            uiBuilder.set("#DataUser0" + i + ".OutlineSize", 0);
            uiBuilder.set("#DataUser0" + i + ".OutlineColor", "#E0B448");
        }

        String[] nameWorld = Objects.requireNonNull(RefactorTool.getPlayer(playerRef).getWorld()).getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }

        uiBuilder.set("#NameMap.TextSpans", Message.raw("Casual | " + worldNameWithSpaces));

        update(true, uiBuilder);
    }

    private List<ScoreEntry> buildRows(GameMatch match) {
        List<ScoreEntry> rows = new ArrayList<>();

        List<PlayerStats> playersList = new ArrayList<>(RefactorTool.getPlayerList(match));
        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for (PlayerStats player : playersList) {
            if (player == null || player.getPlayer() == null || player.getPlayerRef() == null) continue;
            rows.add(new ScoreEntry(
                    player.getPlayer().getDisplayName(),
                    player.getKills(),
                    player.getDeaths(),
                    player.getScore(),
                    player.getPlayerRef()
            ));
        }

        List<DeathmatchBot> bots = new ArrayList<>(DeathmatchBotManager.getBots(match));
        bots.sort((b1, b2) -> Integer.compare(b2.score, b1.score));

        for (DeathmatchBot bot : bots) {
            if (bot == null) continue;
            rows.add(new ScoreEntry(
                    "[BOT] " + bot.displayName,
                    bot.kills,
                    bot.deaths,
                    bot.score,
                    null
            ));
        }

        return rows;
    }

    private record ScoreEntry(String name, int kills, int deaths, int score, PlayerRef playerRef) { }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            remainingSeconds = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch().getTimer();

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
