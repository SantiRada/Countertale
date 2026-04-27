package Tenzinn.Core.UI;

import Tenzinn.Core.PartyManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PartyObject;
import com.hypixel.hytale.server.core.Message;
import Tenzinn.Core.Listeners.MessageListeners;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class QueueHud extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private ScheduledFuture<?> updateTask;
    private long startTime;
    private PlayerRef playerRef;

    private PartyObject myParty;

    public QueueHud(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
        this.playerRef = playerRef;
        this.startTime = System.currentTimeMillis();
    }
    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Lobby/QueueHud.ui");
        uiBuilder = uiCommandBuilder;

        uiBuilder.set("#PartyHUD.Visible", false);
        uiBuilder.set("#LeaveMessage.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_MESSAGE_COMMAND_LEAVE)));

        startUpdating();
    }
    private void startUpdating() {
        updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::updateTimer, 0, 1, TimeUnit.SECONDS);
    }
    public void stopUpdating() {
        if (updateTask != null && !updateTask.isDone()) updateTask.cancel(true);
    }
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
    public void setMapsInfo(List<String> maps) {
        if (uiBuilder == null || maps == null || maps.isEmpty()) return;

        String mapsText = maps.stream()
                .map(m -> Character.toUpperCase(m.charAt(0)) + m.substring(1))
                .collect(Collectors.joining(", "));

        uiBuilder.set("#MapsLabel.TextSpans", Message.raw("Maps: " + mapsText));
        update(true, uiBuilder);
    }
    public void showLoadingMap() {
        if (uiBuilder == null) return;

        uiBuilder.set("#PlayerCountLabel.TextSpans", Message.raw("10/10 Players"));
        uiBuilder.set("#MapsLabel.TextSpans", Message.raw("Loading map..."));
        uiBuilder.set("#LeaveMessage.TextSpans", Message.raw(""));

        update(true, uiBuilder);
    }
    public void setDeleteParty() {
        myParty = null;
        uiBuilder.set("#PartyHUD.Visible", false);

        update(true, uiBuilder);
    }
    public void setDataParty(PartyObject myParty) {
        uiBuilder.set("#PartyHUD.Visible", true);

        this.myParty = myParty;
        uiBuilder.set("#PartyLeader.TextSpans", Message.raw(myParty.leaderUsername));

        update(true, uiBuilder);
    }
}