package Tenzinn.FiveVSfive.UI;

import Tenzinn.Core.UI.ShopEventData;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class EndRoundPage extends InteractiveCustomUIPage<ShopEventData> {

    private UICommandBuilder uiBuilder;

    public EndRoundPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, ShopEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/FVF/EndRound.ui");
        uiBuilder = uiCommandBuilder;

        setData();

        sendUpdate();
    }
    private void setData() {
        int winner = MatchFVF.winner;
        int teamPlayer = MatchFVF.validateTeamMembership(playerRef);

        if (winner == teamPlayer) { uiBuilder.set("#Title.TextSpans", Message.raw("VICTORY")); }
        else { uiBuilder.set("#Title.TextSpans", Message.raw("DEFEAT")); }

        uiBuilder.set("#NumRound.TextSpans", Message.raw("Ronda " + MatchFVF.getNumberRound()));

        sendUpdate();
    }
}