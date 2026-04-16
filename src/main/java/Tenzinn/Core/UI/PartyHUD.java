package Tenzinn.Core.UI;

import Tenzinn.Core.Objects.PartyObject;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PartyHUD extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    public PartyObject myParty;

    public PartyHUD(@NonNullDecl PlayerRef playerRef, PartyObject myParty) { super(playerRef); this.myParty = myParty; }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/PartyHUD.ui");
        uiBuilder = uiCommandBuilder;

        update(true, uiBuilder);
    }

    public void setData() {
        if (uiBuilder == null) return;
        for (PlayerRef player : myParty.players) {
            uiBuilder.appendInline("#Members","Label { " + "TextColor: #FFFFFF; " + "FontSize: 20; " + "RenderBold: false; " +
                            "Text: \"" + player.getUsername() + "\"; " + "}");
        }

        update(true, uiBuilder);
    }
}