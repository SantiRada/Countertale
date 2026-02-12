package Tenzinn.Handle;

import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;

public class CancelHandler implements PlayerPacketFilter {

    private boolean isOpen = false;

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (packet.getId() == 204) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                Store<EntityStore> store = entityRef.getStore();
                World world = store.getExternalData().getWorld();

                world.execute(() -> {
                    try {
                        Player player = store.getComponent(entityRef, Player.getComponentType());
                        if (player != null) {
                            player.getPageManager().setPage(entityRef, store, Page.None);
                            playerRef.sendMessage(Message.raw("¡Inventario bloqueado!"));

                            if (!isOpen) {
                                player.getHudManager().setCustomHud(playerRef, new ScoreboardPage(playerRef));
                                playerRef.sendMessage(Message.raw("¡Scoreboard abierto!").color(Color.yellow));
                            } else {
                                player.getHudManager().setCustomHud(playerRef, new DeathmatchHUD(playerRef));
                                playerRef.sendMessage(Message.raw("¡HUD abierto!").color(Color.yellow));
                            }

                            isOpen = !isOpen;
                        }
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
            }

            return true;
        }

        return false;
    }
}