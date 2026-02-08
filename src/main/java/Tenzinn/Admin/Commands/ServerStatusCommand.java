package Tenzinn.Admin.Commands;

import Tenzinn.Admin.UI.ServerStatusHud;
import Tenzinn.Countertale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.Message;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ServerStatusCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public ServerStatusCommand(String name, String description, Countertale plugin) {
        super(name, description);
        this.plugin = plugin;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        // Obtener el componente Player desde el Store
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) {
            commandContext.sendMessage(Message.raw("Error: No se pudo obtener el componente del jugador."));
            return;
        }

        ServerStatusHud hud = new ServerStatusHud(playerRef);

        // Usar el UUID del PlayerRef como identificador único
        String playerId = playerRef.getUuid().toString();

        // Registrar el HUD en el plugin para actualizaciones periódicas
        plugin.registerServerHud(playerId, hud);

        // Mostrar el HUD
        player.getHudManager().setCustomHud(playerRef, hud);

        commandContext.sendMessage(Message.raw("HUD de estado del servidor activado."));
    }

    @Override
    public String getPermission() { return "countertale.server"; }

    @Override
    public String getName() { return "server"; }
}