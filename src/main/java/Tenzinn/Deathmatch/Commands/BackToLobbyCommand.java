package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.math.vector.Transform;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BackToLobbyCommand extends AbstractPlayerCommand {

    protected Countertale main;

    public BackToLobbyCommand(@NonNullDecl String name, @NonNullDecl String description, Countertale main) {
        super(name, description);
        this.main = main;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        if (!world.getName().equals("Test_Map_Instance")) {
            commandContext.sendMessage(Message.raw("No estás en una partida."));
            return;
        }

        main.getMatchManager().removePlayerFromMatch(playerRef);

        playerRef.sendMessage(Message.raw("Retornando al lobby..."));
        World mainWorld = Universe.get().getDefaultWorld();

        if (mainWorld == null) {
            playerRef.sendMessage(Message.raw("Error: No se pudo encontrar el mundo principal."));
            return;
        }

        Player player = commandContext.senderAs(Player.class);
        if (player != null) player.getInventory().clear();

        Transform lobbySpawn = new Transform(33, 133, -50, 0, 0, 0);

        world.execute(() -> {
            try {
                Teleport teleport = Teleport.createForPlayer(mainWorld, lobbySpawn);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                mainWorld.execute(() -> {
                    try {
                        Thread.sleep(500);

                        Player teleportedPlayer = store.getComponent(ref, Player.getComponentType());
                        if (teleportedPlayer != null) {
                            Inventory inv = teleportedPlayer.getInventory();
                            ItemStack actionBook = new ItemStack("actions_book", 1);
                            inv.getHotbar().addItemStack(actionBook);

                            playerRef.sendMessage(Message.raw("¡Has vuelto al lobby!"));
                        }
                    } catch (Exception e) {
                        main.getLogger().at(java.util.logging.Level.SEVERE).log("Error al dar items del lobby: " + e);
                    }
                });

            } catch (Exception e) {
                playerRef.sendMessage(Message.raw("Error al retornar al lobby"));
                main.getLogger().at(java.util.logging.Level.SEVERE).log("Error en /lobby: " + e);
            }
        });
    }
}