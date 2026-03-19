package Tenzinn.Core.Commands;

import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.FiveVSfive.UI.ScoreboardPageFVF;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ClearHUDCommand extends CommandBase {

    public ClearHUDCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);

        CustomUIHud customHUD = player.getHudManager().getCustomHud();

        if (customHUD != null) {
            if (customHUD instanceof GameHUD gameHUD) {
                gameHUD.clearHUD();
                System.out.println("Game HUD limpio.");
            }

            if (RefactorTool.getModeForPlayer(player) == MapListeners.SpawnMode.DM) {
                if (customHUD instanceof ScoreboardPage scoreboardHUD) {
                    scoreboardHUD.clearHUD();
                    System.out.println("Scoreboard limpio.");
                }
            } else {
                if (customHUD instanceof ScoreboardPageFVF scoreboardHUD) {
                    scoreboardHUD.clearHUD();
                    System.out.println("Scoreboard FVF limpio.");
                }
            }
        }
    }
}