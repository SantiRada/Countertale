package Tenzinn.Deathmatch.UI;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class DeathmatchHUD extends CustomUIHud {

    public DeathmatchHUD(@NonNullDecl PlayerRef playerRef) { super(playerRef); }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Deathmatch.ui");
        update(true, uiCommandBuilder);
    }
}