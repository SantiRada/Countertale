package Tenzinn.Deathmatch.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.Objects;

public class MvpPage extends InteractiveCustomUIPage<MvpEventData> {

    private UICommandBuilder uiBuilder;

    public MvpPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, Tenzinn.Deathmatch.UI.MvpEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/DM/MVP.ui");
        uiBuilder = uiCommandBuilder;

        uiBuilder.set("#Summary.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_TITLE_SUMMARY)));

        uiBuilder.set("#PlayGame.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_BTN_PLAY)));
        uiBuilder.set("#BackLobby.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_BTN_LOBBY)));

        setListeners(uiEventBuilder);

        setLeftData();
        setSummary();
        setScoreboard();

        sendUpdate();
    }
    public void setLeftData() {
        String[] nameWorld = RefactorTool.getPlayer(playerRef).getWorld().getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }

        uiBuilder.set("#MapText.TextSpans", Message.raw("MAP - " + worldNameWithSpaces));
        sendUpdate();
    }
    public void setSummary() {

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        assert playerStats != null;

        sendUpdate();
    }
    public void setScoreboard() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
        int index = 1;

        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for(PlayerStats player : playersList) {
            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(player.getPlayer().getDisplayName()));
            uiBuilder.set("#Kill0" + index + ".TextSpans", Message.raw(String.valueOf(player.getKills())));
            uiBuilder.set("#Death0" + index + ".TextSpans", Message.raw(String.valueOf(player.getDeaths())));
            uiBuilder.set("#Score0" + index + ".TextSpans", Message.raw(String.valueOf(player.getScore())));

            if(player.getPlayerRef().equals(playerRef)) {
                if (index == 1) { uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#27F5A3"); }

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
        }

        sendUpdate();
    }

    private void setListeners(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PlayButton", EventData.of("Action", "play"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LobbyButton", EventData.of("Action", "lobby"));
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, MvpEventData data) {
        String action = data.getAction();

        switch (action) {
            case "play", "lobby":
                GameMatch gameMatch = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
                gameMatch.stopTimer();

                CommandManager.get().handleCommand(playerRef, "lobby");
            break;
        }

        sendUpdate();
    }
}