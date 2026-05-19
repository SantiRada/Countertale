package Tenzinn.Core.Commands;

import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.FiveVSfive.UI.ScoreboardPageFVF;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ClearHUDCommand extends CommandBase {

    public ClearHUDCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        CustomUIHud customHUD = player.getHudManager().getCustomHud();

        if (customHUD != null) {
            if (customHUD instanceof GameHUD gameHUD) {
                gameHUD.clearHUD();
                System.out.println("Game HUD limpio.");
            }

            if (RefactorTool.getModeForPlayer(playerRef) == MapListeners.SpawnMode.DM) {
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