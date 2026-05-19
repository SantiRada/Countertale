package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.UI.CaseInventoryPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

public class ListCasesCommand extends CommandBase {

    public ListCasesCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) return;
        World world = Universe.get().getWorld(worldId);
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new CaseInventoryPage(playerRef));
        });
    }
}
