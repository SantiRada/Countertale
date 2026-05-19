package Tenzinn.Core.Commands.Loot;

import Tenzinn.Core.LootManager;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;

public class ResetLootCommand extends CommandBase {

    public ResetLootCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);
        Player player = RefactorTool.getPlayer(playerRef);

        ArrayList<WeaponStats> currentLoot = LootManager.getStarterKit();

        LootManager.giveLoot(player, currentLoot);
    }
}
