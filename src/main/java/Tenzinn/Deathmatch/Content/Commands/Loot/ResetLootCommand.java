package Tenzinn.Deathmatch.Content.Commands.Loot;

import Tenzinn.Deathmatch.Global.LootManager;
import Tenzinn.Deathmatch.Content.Objects.WeaponStats;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
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
        Player player = commandContext.senderAs(Player.class);
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        assert playerRef != null;

        ArrayList<WeaponStats> currentLoot = LootManager.getStarterKit();

        LootManager.giveLoot(player, currentLoot);
    }
}
