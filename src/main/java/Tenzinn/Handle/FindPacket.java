package Tenzinn.Handle;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class FindPacket implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        int id = packet.getId();
        String name = packet.getClass().getSimpleName();

        if (name.toLowerCase().contains("window") ||
                name.toLowerCase().contains("inventory") ||
                name.toLowerCase().contains("page") ||
                name.toLowerCase().contains("open") ||
                name.toLowerCase().contains("request")) {

            playerRef.sendMessage(Message.raw(
                    "§b[C→S] " + name + " (ID: " + id + ")"
            ));
        }

        return false;
    }
}