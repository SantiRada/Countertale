package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BackToLobbyCommand extends AbstractPlayerCommand {

    public Vector3d spawnLobby = new Vector3d(0, 256, 0);

    protected Countertale main;

    public BackToLobbyCommand(@NonNullDecl String name, @NonNullDecl String description, Countertale main) {
        super(name, description);
        this.main = main;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        System.out.println("[LOBBY] execute() thread: " + Thread.currentThread().getName());
        System.out.println("[LOBBY] world: " + world.getName());

        if (world.getName().equals("default")) {
            commandContext.sendMessage(Message.raw("No estás en una partida."));
            return;
        }

        playerRef.sendMessage(Message.raw("Retornando al lobby..."));
        World mainWorld = Universe.get().getDefaultWorld();

        if (mainWorld == null) {
            playerRef.sendMessage(Message.raw("Error: No se pudo encontrar el mundo principal.").color(Color.red));
            return;
        }

        Object hud = RefactorTool.getCustomHud(playerRef);
        if (hud instanceof DeathmatchHUD deathmatchHUD) { deathmatchHUD.stopTimer(); }

        world.execute(() -> {
            System.out.println("[LOBBY] world.execute() thread: " + Thread.currentThread().getName());
            try {
                Player player = store.getComponent(ref, Player.getComponentType());

                if (player != null) {
                    player.getInventory().clear();

                    Object h = RefactorTool.getCustomHud(playerRef);
                    if (h instanceof DeathmatchHUD deathmatchHUD) { deathmatchHUD.stopTimer(); }

                    player.getHudManager().setCustomHud(playerRef, null);

                    player.getHudManager().resetHud(playerRef);
                }

                main.getMatchManager().removePlayerFromMatch(playerRef);

                Transform spawnPoint = new Transform(spawnLobby.x, spawnLobby.y, spawnLobby.z);
                Teleport teleport = Teleport.createForPlayer(mainWorld, spawnPoint);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

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
                                updatedPlayerRef.sendMessage(Message.raw("¡Has vuelto al lobby!"));
                            }
                        } catch (Exception e) {
                            main.getLogger().at(Level.SEVERE).log("Error en callback lobby: " + e.getMessage());
                        }
                    });
                });

            } catch (Exception e) {
                playerRef.sendMessage(Message.raw("Error al salir").color(Color.RED));
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