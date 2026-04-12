package Tenzinn.Core.UI;

import Tenzinn.Core.Listeners.MessageListeners;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        uiBuilder.set("#LeaveMessage.TextSpans",
                Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_MESSAGE_COMMAND_LEAVE)));

        startUpdating();
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

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

    // ── Actualizaciones externas ──────────────────────────────────────────────

    /** Actualiza el contador de jugadores en el HUD. */
    public void updatePlayerCount(int playerCount) {
        String playerText = String.format("%d/10 Players", playerCount);
        uiBuilder.set("#PlayerCountLabel.TextSpans", Message.raw(playerText));
        update(true, uiBuilder);
    }

    /**
     * Muestra los mapas que el jugador seleccionó al entrar en cola.
     * Capitaliza el primer carácter de cada nombre de mapa.
     *
     * @param maps lista de mapIds (ej. ["dust2", "assault"])
     */
    public void setMapsInfo(List<String> maps) {
        if (uiBuilder == null || maps == null || maps.isEmpty()) return;

        String mapsText = maps.stream()
                .map(m -> Character.toUpperCase(m.charAt(0)) + m.substring(1))
                .collect(Collectors.joining(", "));

        uiBuilder.set("#MapsLabel.TextSpans", Message.raw("Maps: " + mapsText));
        update(true, uiBuilder);
    }

    /**
     * Muestra el estado de carga del escenario en el HUD.
     * Se llama cuando la partida ya tiene 10 jugadores y está esperando
     * que la instancia del mapa termine de cargarse.
     * Oculta el mensaje de /leave (ya no se puede salir) y actualiza las etiquetas.
     */
    public void showLoadingMap() {
        if (uiBuilder == null) return;

        // Actualizar contador a lleno
        uiBuilder.set("#PlayerCountLabel.TextSpans", Message.raw("10/10 Players"));

        // Mostrar estado de carga en lugar de los mapas seleccionados
        uiBuilder.set("#MapsLabel.TextSpans", Message.raw("Cargando escenario..."));

        // Ocultar el mensaje de /leave: la partida ya va a empezar
        uiBuilder.set("#LeaveMessage.TextSpans", Message.raw(""));

        update(true, uiBuilder);
    }

    /** Oculta el HUD de cola y detiene el timer. */
    public void hideQueueUI() {
        stopUpdating();

        try {
            uiBuilder.remove("#QueueHUD");
            update(true, uiBuilder);
        } catch (Exception ignored) { }

        uiBuilder = null;
    }
}
