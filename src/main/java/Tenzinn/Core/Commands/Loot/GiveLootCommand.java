package Tenzinn.Core.Commands.Loot;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Objects.WeaponStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.ArrayList;

public class GiveLootCommand extends CommandBase {

    public GiveLootCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        assert playerRef != null;

        ArrayList<WeaponStats> currentLoot = RefactorTool.getLoot(playerRef);
        if (currentLoot == null || currentLoot.isEmpty()) {
            playerRef.sendMessage(Message.raw("Select weapons in the store to obtain them with this command.").color(Color.yellow));
            return;
        }

        if (currentLoot.size() < 2) {
            playerRef.sendMessage(Message.raw("Select weapons in the store to obtain them with this command.").color(Color.yellow));
            return;
        }

        LootManager.giveLoot(player, currentLoot);
    }
}
