package Tenzinn.Core.Commands.Loot;

import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.ArrayList;

public class GetLootCommand extends CommandBase {

    public GetLootCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        assert playerRef != null;

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        assert  playerStats != null;

        ArrayList<WeaponStats> loot = playerStats.getLoot();
        int i = 1;
        for (WeaponStats item : loot) {
            playerRef.sendMessage(Lang.msg("loot.slot-item", "slot", i, "item", item != null ? item.nameWeapon : "NULL").color(Color.yellow));
            i += 1;
        }
    }
}
