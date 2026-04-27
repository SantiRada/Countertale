package Tenzinn.Core.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Instances.MapVoteStore;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Tools.RefactorTool;
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
    private ScheduledFuture<?> countUpdateTask;
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

        // appendInline about the builder of build(), not in sendUpdate
        appendMaps(uiCommandBuilder);

        // listeners after the #MapN already exist in the builder
        setListeners(uiEventBuilder);

        sendUpdate(buildCommandBuilder(), false);

        // Periodically refresh the per-map player counts so they stay current
        countUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            sendUpdate(buildCommandBuilder(), false);
        }, 3, 3, TimeUnit.SECONDS);
    }

    private int getQueuedPlayers(String mapName) {
        return (int) RefactorTool.playerStatsList.stream()
                .filter(ps -> ps.getCurrentMatch() != null
                        && ps.getCurrentMatch().getState() == GameMatch.MatchState.WAITING
                        && ps.getCurrentMatch().getEligibleMaps().contains(mapName))
                .count();
    }

    private void appendMaps(UICommandBuilder builder) {
        int totalSlots = 3;
        int activeMaps = mapList.size();

        for (int i = 0; i < totalSlots; i++) {
            String anchor = i < totalSlots - 1
                    ? "Anchor: (Width: 270, Height: 420, Right: 16);"
                    : "Anchor: (Width: 270, Height: 420);";

            String inlineUI;

            if (i < activeMaps) {
                String mapName = mapList.get(i);
                String outlineColor = selected.contains(mapName) ? "#FFFFFF" : "#FFFFFF00";
                int queued = getQueuedPlayers(mapName);

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
                                "        Text: \"" + queued + "/10 Players\";\n" +
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

        // Counter
        builder.set("#CountSelected.TextSpans", Message.raw("[" + selected.size() + "/" + mapList.size() + " Maps selected]"));

        // Outlines + live queue counts per map
        for (int i = 0; i < mapList.size(); i++) {
            String outlineColor = selected.contains(mapList.get(i)) ? "#FFFFFF" : "#FFFFFF00";
            builder.set("#Map" + (i + 1) + ".OutlineColor", outlineColor);

            int queued = getQueuedPlayers(mapList.get(i));
            builder.set("#CountPlayers" + (i + 1) + ".TextSpans", Message.raw(queued + "/10 Players"));
        }

        // Tabs DM / FVF
        if (mode.equals("dm")) {
            // DM selected
            builder.set("#ModeDM.Background", "#3A5867");
            builder.set("#ModeDMText.Style.TextColor", "#81D6FD");
            // FVF not selected
            builder.set("#ModeFVF.Background", "#202D3C");
            builder.set("#ModeFVFText.Style.TextColor", "#FFFFFF");
        } else {
            // FVF selected
            builder.set("#ModeFVF.Background", "#3A5867");
            builder.set("#ModeFVFText.Style.TextColor", "#81D6FD");
            // DM not selected
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
                stopCountUpdating();
                player.getPageManager().setPage(ref, store, Page.None);
                break;
            case "play":
                // Rule 2: Without selected maps, it won't let you start.
                if (selected.isEmpty()) {
                    startErrorTimer();
                    return;
                }
                // Save player votes BEFORE queuing; QueueCommand will read them from MapVoteStore
                stopCountUpdating();
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

        // Rule 1: Map toggle
        for (int i = 0; i < mapList.size(); i++) {
            if (action.equalsIgnoreCase(mapList.get(i))) {
                toggleMap(mapList.get(i));
            }
        }

        sendUpdate(buildCommandBuilder(), false);
    }

    private void startErrorTimer() {
        // Cancel previous timer if one existed
        if (errorTimerTask != null && !errorTimerTask.isCancelled()) {
            errorTimerTask.cancel(true);
        }

        errorSeconds = 0;

        errorTimerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            errorSeconds++;

            if (errorSeconds == 1) {
                // First tick: display error message in red
                UICommandBuilder builder = buildCommandBuilder();
                builder.set("#CountSelected.TextSpans", Message.raw("Select maps to start the queue.").color(Color.red));
                sendUpdate(builder, false);
            }

            if (errorSeconds >= 3) {
                // 3 seconds passed: restore normal text and cancel
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

    private void stopCountUpdating() {
        if (countUpdateTask != null && !countUpdateTask.isDone()) {
            countUpdateTask.cancel(true);
            countUpdateTask = null;
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