package Tenzinn.Admin.Commands;

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

public class HideServerStatusCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public HideServerStatusCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) { commandContext.sendMessage(Message.raw("Error: No se pudo obtener el componente del jugador.")); return; }

        String playerId = playerRef.getUuid().toString();

        if (!plugin.hasActiveServerHud(playerId)) return;

        plugin.unregisterServerHud(playerId);
    }

    @Override
    public String getPermission() { return "countertale.hide"; }

    @Override
    public String getName() { return "hide"; }
}