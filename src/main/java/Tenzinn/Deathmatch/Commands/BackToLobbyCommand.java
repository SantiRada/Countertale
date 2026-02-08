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

import java.awt.*;
import java.util.UUID;
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
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        if (!world.getName().equals("Test_Map_Instance")) { commandContext.sendMessage(Message.raw("No estás en una partida.")); return; }

        playerRef.sendMessage(Message.raw("Retornando al lobby..."));
        World mainWorld = Universe.get().getDefaultWorld();

        if (mainWorld == null) { playerRef.sendMessage(Message.raw("Error: No se pudo encontrar el mundo principal.").color(Color.red)); return; }

        Player player = commandContext.senderAs(Player.class);
        if (player != null) player.getInventory().clear();

        Transform spawnPoint = new Transform(0, 244, 0);

        world.execute(() -> {
            try {
                main.getLogger().at(Level.INFO).log("Agregando componente Teleport...");

                Teleport teleport = Teleport.createForPlayer(mainWorld, spawnPoint);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                main.getLogger().at(Level.INFO).log("Teletransporte iniciado, esperando llegada al lobby...");

                CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
                    mainWorld.execute(() -> {
                        try {
                            main.getLogger().at(Level.INFO).log("Jugador en lobby, configurando...");

                            UUID playerUUID = playerRef.getUuid();
                            PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerUUID);

                            if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) {
                                main.getLogger().at(Level.WARNING).log("No se pudo obtener referencia actualizada del jugador");
                                playerRef.sendMessage(Message.raw("Error: No se pudo actualizar tu estado").color(Color.RED));
                                return;
                            }

                            Ref<EntityStore> newRef = updatedPlayerRef.getReference();
                            Store<EntityStore> newStore = newRef.getStore();

                            Player teleportedPlayer = newStore.getComponent(newRef, Player.getComponentType());
                            if (teleportedPlayer != null) {
                                Inventory inv = teleportedPlayer.getInventory();
                                ItemStack actionBook = new ItemStack("actions_book", 1);
                                inv.getHotbar().addItemStack(actionBook);

                                playerRef.sendMessage(Message.raw("¡Has vuelto al lobby!"));

                                main.getLogger().at(Level.INFO).log("⚠️ A punto de llamar removePlayerFromMatch para: " + playerRef.getUuid().toString());
                                boolean removed = main.getMatchManager().removePlayerFromMatch(playerRef);
                                main.getLogger().at(Level.INFO).log("⚠️ Resultado de removePlayerFromMatch: " + removed);
                            } else {
                                main.getLogger().at(Level.WARNING).log("Player component es null");
                            }
                        } catch (Exception e) {
                            main.getLogger().at(java.util.logging.Level.SEVERE).log("Error en callback del lobby: " + e.getMessage(), e);
                            e.printStackTrace();
                        }
                    });
                });

            } catch (Exception e) {
                playerRef.sendMessage(Message.raw("Error al retornar al lobby").color(Color.RED));
                main.getLogger().at(java.util.logging.Level.SEVERE).log("Error en /lobby: " + e.getMessage(), e);
                e.printStackTrace();
            }
        });
    }

    @Override
    public String getPermission() { return "countertale.lobby"; }

    @Override
    public String getName() { return "lobby"; }
}