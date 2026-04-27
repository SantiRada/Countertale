package Tenzinn.Core.Admin.Commands;

import Tenzinn.OrbisOffensive;
import Tenzinn.Core.GameMatch;

import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ForceStartCommand extends AbstractPlayerCommand {

    private final OrbisOffensive plugin;

    public ForceStartCommand(String name, String description, OrbisOffensive plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        GameMatch match = RefactorTool.getPlayerStats(playerRef).getCurrentMatch();

        if (match == null) return;
        if (match.getState() != GameMatch.MatchState.WAITING) return;

        plugin.startMatch(match);
    }

    @Override
    public String getPermission() { return "orbisoffensive.forcestart"; }

    @Override
    public String getName() { return "forcestart"; }
}