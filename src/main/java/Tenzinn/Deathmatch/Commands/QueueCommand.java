package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.Message;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class QueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public QueueCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(Message.raw(String.format("Ya estás en una partida (%d/10 jugadores). Estado: %s",currentMatch.getPlayerCount(),currentMatch.getState())));
            return;
        }

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef);

        commandContext.sendMessage(Message.raw(String.format("Añadido a la cola! Partida %s (%d/10 jugadores)",match.getMatchId().toString().substring(0, 8),match.getPlayerCount())));

        notifyMatchPlayers(match);

        if (match.isFull()) {
            commandContext.sendMessage(Message.raw("¡Partida completa! Iniciando..."));
            plugin.startMatch(match);
        }
    }
    private void notifyMatchPlayers(GameMatch match) {
        String message = String.format("[Partida] Jugadores: %d/10", match.getPlayerCount());

        for (PlayerRef player : match.getPlayers()) { player.sendMessage(Message.raw(message)); }
    }
}