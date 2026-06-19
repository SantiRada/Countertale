package Tenzinn.Core.Commands;

import Tenzinn.Core.Effects.PlayerEntityEffect;
import Tenzinn.Countertale;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.FiveVSfive.UI.ScoreboardPageFVF;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import org.joml.Vector3d;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
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
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        RefactorTool.launchSound(playerRef, "fail");

        if (world.getName().equals(Universe.get().getDefaultWorld().getName())) {
            commandContext.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_COMMAND_LOBBY_INLOBBY));
            return;
        }

        playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_BACK_TO_LOBBY));
        World mainWorld = Universe.get().getDefaultWorld();

        if (mainWorld == null) return;

        world.execute(() -> {
            try {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().setPage(ref, store, Page.None);
                    clearMatchHud(playerRef, player);
                }

                main.getMatchManager().removePlayerFromMatch(playerRef);

                Vector3d spawnLobby = MapListeners.getLobby();

                Teleport teleport = Teleport.createForPlayer(mainWorld, spawnLobby, Rotation3f.ZERO);
                store.addComponent(ref, Teleport.getComponentType(), teleport);

                CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                    mainWorld.execute(() -> {
                        try {
                            PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerRef.getUuid());
                            if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) return;

                            Store<EntityStore> lobbyStore = mainWorld.getEntityStore().getStore();
                            Player lobbyPlayer = lobbyStore.getComponent(updatedPlayerRef.getReference(), Player.getComponentType());

                            if (lobbyPlayer != null) {
                                LootManager.getLobbyLoot(lobbyPlayer);

                                PlayerEntityEffect.clearAllEffects(lobbyPlayer, lobbyStore);
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

    private void clearMatchHud(PlayerRef playerRef, Player player) {
        String hudId = playerRef.getUuid().toString();
        CustomUIHud customHUD = player.getHudManager().getCustomHud(hudId);
        if (customHUD == null) return;

        if (customHUD instanceof GameHUD gameHUD) gameHUD.clearHUD();
        else if (customHUD instanceof ScoreboardPage scoreboardHUD) scoreboardHUD.clearHUD();
        else if (customHUD instanceof ScoreboardPageFVF scoreboardHUD) scoreboardHUD.clearHUD();

        player.getHudManager().removeCustomHud(playerRef, hudId);
    }

    @Override
    public String getPermission() { return "countertale.lobby"; }

    @Override
    public String getName() { return "lobby"; }
}
