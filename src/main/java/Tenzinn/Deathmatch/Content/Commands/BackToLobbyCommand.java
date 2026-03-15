package Tenzinn.Deathmatch.Content.Commands;

import Tenzinn.Countertale;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Deathmatch.Global.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BackToLobbyCommand extends AbstractPlayerCommand {

    protected Countertale main;

    public BackToLobbyCommand(@NonNullDecl String name, @NonNullDecl String description, Countertale main) {
        super(name, description);
        this.main = main;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        RefactorTool.launchSound(playerRef, "fail");

        System.out.println("[LOBBY] execute() thread: " + Thread.currentThread().getName());
        System.out.println("[LOBBY] world: " + world.getName());

        if (world.getName().equals("default")) {
            commandContext.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_COMMAND_LOBBY_INLOBBY)));
            return;
        }

        playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_BACK_TO_LOBBY)));
        World mainWorld = Universe.get().getDefaultWorld();

        if (mainWorld == null) return;

        CommandManager.get().handleCommand(playerRef, "clearhud");

        world.execute(() -> {
            System.out.println("[LOBBY] world.execute() thread: " + Thread.currentThread().getName());
            try {
                main.getMatchManager().removePlayerFromMatch(playerRef);

                Vector3d spawnLobby = MapListeners.getLobby().toVector3d();

                Transform spawnPoint = new Transform(spawnLobby.x, spawnLobby.y, spawnLobby.z);
                Teleport teleport = Teleport.createForPlayer(mainWorld, spawnPoint);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                // El delayed executor solo para el loot del lobby
                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    mainWorld.execute(() -> {
                        try {
                            PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerRef.getUuid());
                            if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) return;

                            Store<EntityStore> lobbyStore = mainWorld.getEntityStore().getStore();
                            Player lobbyPlayer = lobbyStore.getComponent(updatedPlayerRef.getReference(), Player.getComponentType());

                            if (lobbyPlayer != null) {
                                lobbyPlayer.getInventory().clear();
                                lobbyPlayer.getInventory().getHotbar().addItemStack(new ItemStack("actions_book", 1));
                            }
                        } catch (Exception e) {
                            main.getLogger().at(Level.SEVERE).log("Error en callback lobby: " + e.getMessage());
                        }
                    });
                });

            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("Error en /lobby: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public String getPermission() { return "countertale.lobby"; }

    @Override
    public String getName() { return "lobby"; }
}