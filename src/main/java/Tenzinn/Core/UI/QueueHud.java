package Tenzinn.Core.UI;

import Tenzinn.Core.PartyManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PartyObject;
import Tenzinn.Core.Localization.Lang;
import com.hypixel.hytale.server.core.Message;
import Tenzinn.Core.Listeners.MessageListeners;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

public class QueueHud extends CustomUIHud {

    private static final Set<QueueHud> ACTIVE_HUDS = ConcurrentHashMap.newKeySet();
    private static ScheduledFuture<?> sharedUpdateTask;

    private UICommandBuilder uiBuilder;
    private long startTime;
    private PlayerRef playerRef;
    private String lastTimerText = "";

    private PartyObject myParty;

    public QueueHud(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, playerRef.getUuid().toString());
        this.playerRef = playerRef;
        this.startTime = System.currentTimeMillis();
    }
    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Lobby/QueueHud.ui");
        uiBuilder = uiCommandBuilder;

        uiBuilder.set("#PartyHUD.Visible", false);
        uiBuilder.set("#LeaveMessage.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_MESSAGE_COMMAND_LEAVE));

        startUpdating();
    }
    private void startUpdating() {
        registerTicker(this);
    }
    public void stopUpdating() {
        unregisterTicker(this);
    }
    private void updateTimer() {
        if (uiBuilder == null) {
            stopUpdating();
            return;
        }

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        int minutes = (int) (elapsedSeconds / 60);
        int seconds = (int) (elapsedSeconds % 60);

        String timeText = String.format("%02d:%02d", minutes, seconds);
        if (timeText.equals(lastTimerText)) return;

        lastTimerText = timeText;
        uiBuilder.set("#TimerLabel.TextSpans", Message.raw(timeText));
        update(true, uiBuilder);
    }
    public void updatePlayerCount(int playerCount) {
        if (uiBuilder == null) return;
        String playerText = String.format("%d/10 Players", playerCount);
        uiBuilder.set("#PlayerCountLabel.TextSpans", Message.raw(playerText));
        update(true, uiBuilder);
    }
    public void setMapsInfo(List<String> maps) {
        if (uiBuilder == null || maps == null || maps.isEmpty()) return;

        String mapsText = maps.stream()
                .map(m -> Character.toUpperCase(m.charAt(0)) + m.substring(1))
                .collect(Collectors.joining(", "));

        uiBuilder.set("#MapsLabel.TextSpans", Lang.msg("ui.queue.maps", "maps", mapsText));
        update(true, uiBuilder);
    }
    public void showLoadingMap() {
        if (uiBuilder == null) return;

        // Actualizar contador a lleno
        uiBuilder.set("#PlayerCountLabel.TextSpans", Lang.msg("ui.queue.players-full"));

        // Mostrar estado de carga en lugar de los mapas seleccionados
        uiBuilder.set("#MapsLabel.TextSpans", Lang.msg("ui.queue.loading-map"));

        // Ocultar el mensaje de /leave: la partida ya va a empezar
        uiBuilder.set("#LeaveMessage.TextSpans", Message.raw(""));

        update(true, uiBuilder);
    }
    public void setDeleteParty() {
        if (uiBuilder == null) return;
        myParty = null;
        uiBuilder.set("#PartyHUD.Visible", false);

        update(true, uiBuilder);
    }
    public void setDataParty(PartyObject myParty) {
        if (uiBuilder == null) return;
        uiBuilder.set("#PartyHUD.Visible", true);

        this.myParty = myParty;
        uiBuilder.set("#PartyLeader.TextSpans", Message.raw(myParty.leaderUsername));

        update(true, uiBuilder);
    }

    private static synchronized void registerTicker(QueueHud hud) {
        ACTIVE_HUDS.add(hud);
        if (sharedUpdateTask == null || sharedUpdateTask.isDone()) {
            sharedUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(QueueHud::tickAll, 0, 1, TimeUnit.SECONDS);
        }
    }

    private static synchronized void unregisterTicker(QueueHud hud) {
        ACTIVE_HUDS.remove(hud);
        if (ACTIVE_HUDS.isEmpty() && sharedUpdateTask != null && !sharedUpdateTask.isDone()) {
            sharedUpdateTask.cancel(false);
            sharedUpdateTask = null;
        }
    }

    private static void tickAll() {
        for (QueueHud hud : ACTIVE_HUDS) {
            try {
                hud.updateTimer();
            } catch (Exception e) {
                unregisterTicker(hud);
            }
        }
    }

    public static synchronized void clearRuntimeState() {
        ACTIVE_HUDS.clear();
        if (sharedUpdateTask != null && !sharedUpdateTask.isDone()) {
            sharedUpdateTask.cancel(false);
        }
        sharedUpdateTask = null;
    }
}
