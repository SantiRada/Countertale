package Tenzinn.Deathmatch.UI;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.PlayerStats;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DeathmatchHUD extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerStats playerStats;
    private PlayerRef playerRef;
    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600;

    public DeathmatchHUD(@NonNullDecl PlayerRef playerRef) { super(playerRef); this.playerRef = playerRef; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Deathmatch.ui");
        uiBuilder = uiCommandBuilder;

        update(true, uiBuilder);

        playerStats = RefactorTool.getPlayerStats(playerRef);

        setTimer();
        setData();
    }

    public void setData() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList();
        int index = 1;

        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for(PlayerStats player : playersList) {
            uiBuilder.set("#User0" + index + ".TextSpans", Message.raw(String.valueOf(player.getKills())));

            if(player.getPlayerRef().equals(playerRef)) {
                if (index == 1) { uiBuilder.set("#Name0" + index + ".Background", "#27F5A3"); }
                else { uiBuilder.set("#Name0" + index + ".Background", "#E0B448"); }
            }

            index += 1;
        }

        // Fallback
        for (int i = index; i <= 10; i++) {
            uiBuilder.set("#User0" + i + ".TextSpans", Message.raw(""));
            uiBuilder.set("#Name0" + i + ".Background", "#00000040");
        }

        update(true, uiBuilder);
    }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            remainingSeconds = RefactorTool.getPlayerStats(playerRef).getCurrentMatch().getTimer();

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            String timerText = String.format("%02d:%02d", minutes, seconds);

            uiBuilder.set("#TextTimer.TextSpans", Message.raw(timerText));

            update(true, uiBuilder);
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void clearHUD() {
        if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false);

        uiBuilder.remove("#DeathmatchUI");
        update(true, uiBuilder);
    }
}