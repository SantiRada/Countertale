package Tenzinn.Core.Handle;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.FiveVSfive.UI.ScoreboardPageFVF;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CancelHandler implements PlayerPacketFilter {

    private boolean isOpen = false;

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {

        if (packet.getId() == 204) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null && entityRef.isValid()) {
                Store<EntityStore> store = entityRef.getStore();
                World world = store.getExternalData().getWorld();

                if(world.getName().equals("default")) { return false; }

                world.execute(() -> {
                    String mode = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch().getMode();

                    try {
                        Player player = store.getComponent(entityRef, Player.getComponentType());
                        if (player != null) {
                            player.getPageManager().setPage(entityRef, store, Page.None);
                            if (!isOpen) {
                                GameHUD hud = (GameHUD)player.getHudManager().getCustomHud();
                                hud.clearHUD();

                                if (mode.equalsIgnoreCase("dm")) {
                                    // Game in DM
                                    player.getHudManager().setCustomHud(playerRef, new ScoreboardPage(playerRef));
                                } else{
                                    // Game in FVF
                                    player.getHudManager().setCustomHud(playerRef, new ScoreboardPageFVF(playerRef));
                                }
                            }
                            else {
                                if(player.getHudManager().getCustomHud() != null){
                                    if (mode.equalsIgnoreCase("dm")) {
                                        ScoreboardPage hud = (ScoreboardPage)player.getHudManager().getCustomHud();
                                        hud.clearHUD();
                                    } else{
                                        ScoreboardPageFVF hud = (ScoreboardPageFVF)player.getHudManager().getCustomHud();
                                        hud.clearHUD();
                                    }
                                }

                                player.getHudManager().setCustomHud(playerRef, new GameHUD(playerRef));
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