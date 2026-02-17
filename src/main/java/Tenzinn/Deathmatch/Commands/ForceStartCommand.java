package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class ForceStartCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public ForceStartCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        GameMatch match = RefactorTool.getPlayerStats(playerRef).getCurrentMatch();

        if (match == null) { commandContext.sendMessage(Message.raw("No estás en ninguna partida. Usa /queue primero.").color(Color.YELLOW)); return; }
        else { commandContext.sendMessage(Message.raw("Se detectó tu partida...")); }

        if (match.getState() != GameMatch.MatchState.WAITING) { commandContext.sendMessage(Message.raw("La partida ya no está en estado WAITING.")); return; }

        int currentPlayers = match.getPlayerCount();

        commandContext.sendMessage(Message.raw(String.format("[DEBUG] Forzando inicio de partida con %d jugador(es)...", currentPlayers)).color(Color.ORANGE));

        plugin.startMatch(match);
    }

    @Override
    public String getPermission() { return "countertale.forcestart"; }

    @Override
    public String getName() { return "forcestart"; }
}