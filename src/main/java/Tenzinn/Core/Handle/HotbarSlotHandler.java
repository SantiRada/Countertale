package Tenzinn.Core.Handle;

import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class HotbarSlotHandler implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof SyncInteractionChains syncPacket)) { return false; }

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType == InteractionType.SwapFrom && chain.data != null && chain.initial) {
                int toSlot = chain.data.targetSlot;
                handleSlotSwap(playerRef, toSlot);

                return false;
            }
        }

        return false;
    }

    private void handleSlotSwap(PlayerRef playerRef, int toSlot) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) return;

        Store<EntityStore> store = entityRef.getStore();
        World world = store.getExternalData().getWorld();

        if (world.getName().equals("default")) return;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            if (playerRef.getWorldUuid() == null) return;

            World latestWorld = Universe.get().getWorld(playerRef.getWorldUuid());
            if (latestWorld == null) return;

            latestWorld.execute(() -> RefactorTool.setChangesInSlots(toSlot + 1, playerRef));
        }, 50, TimeUnit.MILLISECONDS);
    }
}