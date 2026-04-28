package Tenzinn.Deathmatch.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Deathmatch.Bots.DeathmatchBot;
import Tenzinn.Deathmatch.Bots.DeathmatchBotManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MvpPage extends InteractiveCustomUIPage<MvpEventData> {

    private UICommandBuilder uiBuilder;

    public MvpPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CantClose, Tenzinn.Deathmatch.UI.MvpEventData.CODEC); }

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
        if (playerStats == null) return;

        uiBuilder.set("#DamageCaused.TextSpans", Message.raw("Kills"));
        uiBuilder.set("#DamageCausedText.TextSpans", Message.raw(String.valueOf(playerStats.getKills())));

        uiBuilder.set("#DamageReceived.TextSpans", Message.raw("Deaths"));
        uiBuilder.set("#DamageReceivedText.TextSpans", Message.raw(String.valueOf(playerStats.getDeaths())));

        uiBuilder.set("#MeleeDamage.TextSpans", Message.raw("Score"));
        uiBuilder.set("#MeleeDamageValue.TextSpans", Message.raw(String.valueOf(playerStats.getScore())));

        sendUpdate();
    }
    public void setScoreboard() {
        PlayerStats viewerStats = RefactorTool.getPlayerStats(playerRef);
        if (viewerStats == null || viewerStats.getCurrentMatch() == null) return;

        int index = 1;
        List<ScoreEntry> rows = buildRows(viewerStats.getCurrentMatch());

        for (ScoreEntry row : rows) {
            if (index > 10) break;

            uiBuilder.set("#Name0" + index + ".TextSpans", Message.raw(row.name()));
            uiBuilder.set("#Kill0" + index + ".TextSpans", Message.raw(String.valueOf(row.kills())));
            uiBuilder.set("#Death0" + index + ".TextSpans", Message.raw(String.valueOf(row.deaths())));
            uiBuilder.set("#Score0" + index + ".TextSpans", Message.raw(String.valueOf(row.score())));
            uiBuilder.set("#DataUser0" + index + ".OutlineSize", 0);
            uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#E0B448");

            PlayerRef rowPlayerRef = row.playerRef();
            if (rowPlayerRef != null && rowPlayerRef.equals(playerRef)) {
                if (index == 1) {
                    uiBuilder.set("#DataUser0" + index + ".OutlineColor", "#27F5A3");
                }

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
            uiBuilder.set("#DataUser0" + i + ".OutlineSize", 0);
            uiBuilder.set("#DataUser0" + i + ".OutlineColor", "#E0B448");
        }

        sendUpdate();
    }

    private List<ScoreEntry> buildRows(GameMatch match) {
        List<ScoreEntry> rows = new ArrayList<>();

        List<PlayerStats> playersList = new ArrayList<>(RefactorTool.getPlayerList(match));
        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        for (PlayerStats player : playersList) {
            if (player == null || player.getPlayer() == null || player.getPlayerRef() == null) continue;
            rows.add(new ScoreEntry(
                    player.getPlayer().getDisplayName(),
                    player.getKills(),
                    player.getDeaths(),
                    player.getScore(),
                    player.getPlayerRef()
            ));
        }

        List<DeathmatchBot> bots = new ArrayList<>(DeathmatchBotManager.getBots(match));
        bots.sort((b1, b2) -> Integer.compare(b2.score, b1.score));

        for (DeathmatchBot bot : bots) {
            if (bot == null) continue;
            rows.add(new ScoreEntry(
                    "[BOT] " + bot.displayName,
                    bot.kills,
                    bot.deaths,
                    bot.score,
                    null
            ));
        }

        return rows;
    }

    private record ScoreEntry(String name, int kills, int deaths, int score, PlayerRef playerRef) { }

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
