package Tenzinn.FiveVSfive.UI;

import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;

public class EndMatchPage extends InteractiveCustomUIPage<EndMatchPage.EndMatchEventData> {

    private final int winningTeam;

    public EndMatchPage(PlayerRef playerRef, int winningTeam) {
        super(playerRef, CustomPageLifetime.CantClose, EndMatchEventData.CODEC);
        this.winningTeam = winningTeam;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Game/FVF/EndMatch.ui");

        int playerTeam = MatchFVF.validateTeamMembership(playerRef);
        boolean isWinner = playerTeam == winningTeam;

        commandBuilder.set("#Title.TextSpans", Message.raw(isWinner ? "VICTORY" : "DEFEAT"));
        commandBuilder.set("#Title.Style.TextColor", isWinner ? "#00FF00" : "#FF0000");

        int rounds1 = MatchFVF.numRoundsPerTeam.isEmpty() ? 0 : MatchFVF.numRoundsPerTeam.get(0);
        int rounds2 = MatchFVF.numRoundsPerTeam.size() < 2 ? 0 : MatchFVF.numRoundsPerTeam.get(1);
        commandBuilder.set("#Score.TextSpans", Message.raw(rounds1 + " - " + rounds2));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#LobbyButton",
                EventData.of("Action", "Lobby"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull EndMatchEventData data) {
        if ("Lobby".equals(data.action)) {
            // Dismiss the page server-side first — CantClose only blocks ESC on the client,
            // but a programmatic setPage(None) always works. Without this the page stays
            // pinned after the world transition and Hytale gets stuck on a loading screen.
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            CommandManager.get().handleCommand(playerRef, "lobby");
        }
    }

    // ── Event codec ───────────────────────────────────────────────────────────
    public static class EndMatchEventData {
        public static final BuilderCodec CODEC = BuilderCodec
                .builder(EndMatchEventData.class, EndMatchEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (entry, s) -> entry.action = s,
                        (entry) -> entry.action)
                .add()
                .build();

        private String action;
    }
}
