package Tenzinn.Deathmatch.UI;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class QueueHud extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private ScheduledFuture<?> updateTask;
    private long startTime;

    public QueueHud(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Lobby/QueueHud.ui");
        uiBuilder = uiCommandBuilder;

        startUpdating();
    }
    private void startUpdating() { updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::updateTimer,0,1,TimeUnit.SECONDS); }
    public void stopUpdating() { if (updateTask != null && !updateTask.isDone()) updateTask.cancel(false); }
    private void updateTimer() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        int minutes = (int) (elapsedSeconds / 60);
        int seconds = (int) (elapsedSeconds % 60);

        String timeText = String.format("%02d:%02d", minutes, seconds);

        uiBuilder.set("#TimerLabel.TextSpans", Message.raw(timeText));
        update(true, uiBuilder);
    }
    public void updatePlayerCount(int playerCount) {
        String playerText = String.format("%d/10 Players", playerCount);
        uiBuilder.set("#PlayerCountLabel.TextSpans", Message.raw(playerText));

        update(true, uiBuilder);
    }
    public void hideQueueUI() {
        stopUpdating();

        uiBuilder.remove("#QueueHUD");
        update(true, uiBuilder);
    }
}