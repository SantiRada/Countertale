package Tenzinn.Deathmatch.UI;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DeathmatchHUD extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600; // 10:00

    public DeathmatchHUD(@NonNullDecl PlayerRef playerRef) { super(playerRef); }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Deathmatch.ui");
        uiBuilder = uiCommandBuilder;

        update(true, uiBuilder);

        startData();
    }

    private void startData() {
        startTimer();

        // Falta ver como cambiar los valores de kills por jugador
    }
    private void startTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            if (remainingSeconds <= 0) {
                stopTimer();
                return;
            }

            remainingSeconds--;

            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            String timeText = String.format("%02d:%02d", minutes, seconds);

            uiBuilder.set("#TextTimer.TextSpans", Message.raw(timeText));
            update(true, uiBuilder);

        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
        }
    }

    public void clearHUD() {
        stopTimer();

        uiBuilder.remove("#DeathmatchUI");
        update(true, uiBuilder);
    }
}