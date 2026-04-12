package Tenzinn.Core.UI;

import Tenzinn.Core.Instances.MapVoteStore;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.FiveVSfive.UI.Events.ModesEventData;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class ModesPage extends InteractiveCustomUIPage<ModesEventData> {

    private String mode = "dm";
    private List<String> selected = new ArrayList<>();
    private List<String> mapList = new ArrayList<>();
    private ScheduledFuture<?> errorTimerTask;
    private int errorSeconds = 0;

    private UICommandBuilder uiBuilder;

    public ModesPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, ModesEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Modes.ui");
        uiBuilder = uiCommandBuilder;

        List<String> totalList = MapListeners.getMapNames().stream().toList();
        for (String map : totalList) {
            mapList.add(map);
            selected.add(map);
        }

        // appendInline sobre el builder del build(), no en sendUpdate
        appendMaps(uiCommandBuilder);

        // listeners después de que los #MapN ya existen en el builder
        setListeners(uiEventBuilder);

        sendUpdate(buildCommandBuilder(), false);
    }

    private void appendMaps(UICommandBuilder builder) {
        int totalSlots = 5;
        int activeMaps = mapList.size();

        for (int i = 0; i < totalSlots; i++) {
            String anchor = i < totalSlots - 1
                    ? "Anchor: (Width: 196, Height: 420, Right: 16);"
                    : "Anchor: (Width: 196, Height: 420);";

            String inlineUI;

            if (i < activeMaps) {
                String mapName = mapList.get(i);
                String outlineColor = selected.contains(mapName) ? "#FFFFFF" : "#FFFFFF00";

                inlineUI =
                        "Button #Map" + (i + 1) + " {\n" +
                                "    LayoutMode: Bottom;\n" +
                                "    Padding: (Bottom: 24);\n" +
                                "    OutlineSize: 2;\n" +
                                "    OutlineColor: " + outlineColor + ";\n" +
                                "    " + anchor + "\n" +
                                "\n" +
                                "    Label #NameMap" + (i + 1) + " {\n" +
                                "        Style: (TextColor: #FFFFFF, RenderBold: true, Alignment: Center, FontSize: 16);\n" +
                                "        Text: \"" + mapName + "\";\n" +
                                "    }\n" +
                                "\n" +
                                "    Label #CountPlayers" + (i + 1) + " {\n" +
                                "        Style: (TextColor: #FFFFFF(0.5), Alignment: Center, FontSize: 12);\n" +
                                "        Text: \"0/10 Players\";\n" +
                                "    }\n" +
                                "}";
            } else {
                inlineUI =
                        "Group #Map" + (i + 1) + " {\n" +
                                "    LayoutMode: Middle;\n" +
                                "    " + anchor + "\n" +
                                "\n" +
                                "    Label {\n" +
                                "        Style: (TextColor: #FFFFFF(0.75), RenderBold: true, FontSize: 16, Alignment: Center);\n" +
                                "        Text: \"Coming Soon\";\n" +
                                "    }\n" +
                                "}";
            }

            builder.appendInline("#Maps", inlineUI);

            builder.set("#Map" + (i + 1) + ".Background", Value.ref("Game/Modes.ui", "Map" + (i + 1)));
        }
    }

    private UICommandBuilder buildCommandBuilder() {
        UICommandBuilder builder = new UICommandBuilder();

        // Contador
        builder.set("#CountSelected.TextSpans", Message.raw("[" + selected.size() + "/" + mapList.size() + " Maps selected]"));

        // Outlines de mapas según selección
        for (int i = 0; i < mapList.size(); i++) {
            String outlineColor = selected.contains(mapList.get(i)) ? "#FFFFFF" : "#FFFFFF00";
            builder.set("#Map" + (i + 1) + ".OutlineColor", outlineColor);
        }

        // Tabs DM / FVF
        if (mode.equals("dm")) {
            // DM seleccionado
            builder.set("#ModeDM.Background", "#3A5867");
            builder.set("#ModeDMText.Style.TextColor", "#81D6FD");
            // FVF no seleccionado
            builder.set("#ModeFVF.Background", "#202D3C");
            builder.set("#ModeFVFText.Style.TextColor", "#FFFFFF");
        } else {
            // FVF seleccionado
            builder.set("#ModeFVF.Background", "#3A5867");
            builder.set("#ModeFVFText.Style.TextColor", "#81D6FD");
            // DM no seleccionado
            builder.set("#ModeDM.Background", "#202D3C");
            builder.set("#ModeDMText.Style.TextColor", "#FFFFFF");
        }

        return builder;
    }

    private void setListeners(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeDM", EventData.of("Action", "dm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeFVF", EventData.of("Action", "fvf"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Back", EventData.of("Action", "back"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Game", EventData.of("Action", "play"));

        for (int i = 0; i < mapList.size(); i++) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Map" + (i + 1), EventData.of("Action", mapList.get(i)));
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, ModesEventData data) {
        String action = data.getAction();
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        switch (action.toLowerCase()) {
            case "back":
                player.getPageManager().setPage(ref, store, Page.None);
                break;
            case "play":
                // Regla 2: sin mapas seleccionados no deja iniciar
                if (selected.isEmpty()) {
                    startErrorTimer();
                    return;
                }
                // Guardar los votos del jugador ANTES de encolar; QueueCommand los leerá desde MapVoteStore
                MapVoteStore.setVotes(playerRef, new ArrayList<>(selected));
                CommandManager.get().handleCommand(playerRef, "queue --mode=" + mode);
                player.getPageManager().setPage(ref, store, Page.None);
                break;
            case "dm":
                mode = "dm";
                resetList();
                break;
            case "fvf":
                mode = "fvf";
                resetList();
                break;
        }

        // Regla 1: toggle de mapa
        for (int i = 0; i < mapList.size(); i++) {
            if (action.equalsIgnoreCase(mapList.get(i))) {
                toggleMap(mapList.get(i));
            }
        }

        sendUpdate(buildCommandBuilder(), false);
    }

    private void startErrorTimer() {
        // Cancelar timer previo si existía
        if (errorTimerTask != null && !errorTimerTask.isCancelled()) {
            errorTimerTask.cancel(true);
        }

        errorSeconds = 0;

        errorTimerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            errorSeconds++;

            if (errorSeconds == 1) {
                // Primer tick: mostrar mensaje de error en rojo
                UICommandBuilder builder = buildCommandBuilder();
                builder.set("#CountSelected.TextSpans", Message.raw("Selecciona mapas para iniciar la cola").color(Color.red));
                sendUpdate(builder, false);
            }

            if (errorSeconds >= 3) {
                // Pasaron 3 segundos: restaurar texto normal y cancelar
                sendUpdate(buildCommandBuilder(), false);
                stopErrorTimer();
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopErrorTimer() {
        if (errorTimerTask != null) {
            errorTimerTask.cancel(true);
            errorTimerTask = null;
            errorSeconds = 0;
        }
    }

    private void toggleMap(String map) {
        if (selected.contains(map)) {
            selected.remove(map);
        } else {
            selected.add(map);
        }
    }

    private void resetList() { selected = new ArrayList<>(mapList); }
}