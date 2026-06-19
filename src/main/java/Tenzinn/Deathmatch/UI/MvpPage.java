package Tenzinn.Deathmatch.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
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

        uiBuilder.set("#Summary.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_TITLE_SUMMARY));

        uiBuilder.set("#PlayGame.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_BTN_PLAY));
        uiBuilder.set("#BackLobby.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_BTN_LOBBY));

        setListeners(uiEventBuilder);

        setLeftData();
        setSummary();
        setScoreboard();
        setCaseReward();

        sendUpdate();
    }
    public void setLeftData() {
        String[] nameWorld = RefactorTool.getPlayer(playerRef).getWorld().getName().split("_");
        String worldNameWithSpaces = String.join(" ", nameWorld);
        int withInstance = worldNameWithSpaces.toLowerCase().indexOf("instance");
        if (withInstance != -1) { worldNameWithSpaces = worldNameWithSpaces.substring(0, withInstance).trim(); }

        uiBuilder.set("#MapText.TextSpans", Lang.msg("ui.map.label", "map", worldNameWithSpaces));
        sendUpdate();
    }
    public void setSummary() {

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        assert playerStats != null;

        uiBuilder.set("#DamageCaused.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_DAMAGE_CAUSED));
        uiBuilder.set("#DamageReceived.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_DAMAGE_RECEIVED));
        uiBuilder.set("#MeleeDamage.TextSpans", MessageListeners.message(MessageListeners.MessageKey.UI_MELEE_DAMAGE));

        uiBuilder.set("#DamageCausedText.TextSpans", Message.raw(String.valueOf(playerStats.getDamageCaused())));
        uiBuilder.set("#DamageReceivedText.TextSpans", Message.raw(String.valueOf(playerStats.getDamageReceived())));
        uiBuilder.set("#MeleeDamageValue.TextSpans", Message.raw(String.valueOf(playerStats.getMeleeDamage())));

        sendUpdate();
    }

    private void setCaseReward() {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        boolean hasCaseDrop = playerStats != null && playerStats.hasPendingCase;

        uiBuilder.set("#CaseRewardIcon.Visible", hasCaseDrop);
        uiBuilder.set("#CaseRewardTitle.TextSpans", hasCaseDrop
                ? Lang.msg("ui.case.reward-title")
                : Lang.msg("ui.case.no-reward-title"));
        uiBuilder.set("#CaseRewardDescription.TextSpans", hasCaseDrop
                ? Lang.msg("ui.case.reward-description")
                : Lang.msg("ui.case.no-reward-description"));
    }
    public void setScoreboard() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
        int index = 1;

        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for(PlayerStats player : playersList) {
            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(player.getPlayerRef().getUsername()));
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

                PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
                if (stats != null) stats.hasPendingCase = false;

                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) player.getPageManager().setPage(ref, store, Page.None);

                CommandManager.get().handleCommand(playerRef, "lobby");
                return;
        }

        sendUpdate();
    }
}
