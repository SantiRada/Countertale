package Tenzinn.Deathmatch.Commands;

import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import java.awt.*;

public class ClearHUDCommand extends CommandBase {

    public ClearHUDCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);

        CustomUIHud customHUD = player.getHudManager().getCustomHud();

        if (customHUD != null) {
            if (customHUD instanceof DeathmatchHUD deathmatchHUD) {
                deathmatchHUD.clearHUD();
                player.sendMessage(Message.raw("Deathmatch HUD limpio.").color(Color.cyan));
                System.out.println("Deathmatch HUD limpio.");
            }

            if (customHUD instanceof ScoreboardPage scoreboardHUD) {
                scoreboardHUD.clearHUD();
                player.sendMessage(Message.raw("Scoreboard HUD limpio.").color(Color.cyan));
                System.out.println("Scoreboard HUD limpio.");
            }
        }
    }
}