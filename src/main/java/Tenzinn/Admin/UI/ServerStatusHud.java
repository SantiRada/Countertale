package Tenzinn.Admin.UI;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ServerStatusHud extends CustomUIHud {

    protected UICommandBuilder uiBuilder;

    public ServerStatusHud(@NonNullDecl PlayerRef playerRef) { super(playerRef); }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Server/Status.ui");
        uiBuilder = uiCommandBuilder;

        updateStats();
    }

    public void updateStats() {
        Runtime runtime = Runtime.getRuntime();

        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        double tps = 30.0;
        String playersInfo = "N/A";
        String entitiesInfo = "N/A";
        String chunksInfo = "N/A";

        // Actualizar labels
        uiBuilder.set("#TPSLabel.TextSpans", Message.raw(String.format("TPS: %.1f / 30.0", tps)));
        uiBuilder.set("#MemoryLabel.TextSpans", Message.raw(String.format("Memory: %d MB / %d MB (%.1f%%)", usedMemory, maxMemory, (usedMemory * 100.0 / maxMemory))));
        uiBuilder.set("#PlayersLabel.TextSpans", Message.raw(String.format("Players: %s/100", playersInfo)));
        uiBuilder.set("#EntitiesLabel.TextSpans", Message.raw(String.format("Entities: %s", entitiesInfo)));
        uiBuilder.set("#ChunksLabel.TextSpans", Message.raw(String.format("Chunks: %s", chunksInfo)));

        update(true, uiBuilder);
    }
    public void hideStats(){
        uiBuilder.remove("#ServerStatusPanel");
        update(true, uiBuilder);
    }
}