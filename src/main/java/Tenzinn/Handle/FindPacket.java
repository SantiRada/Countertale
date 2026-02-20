package Tenzinn.Handle;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;

import javax.annotation.Nonnull;
import java.awt.*;

public class FindPacket implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        int id = packet.getId();
        String name = packet.getClass().getSimpleName();

        if (id != 3 && id != 108) { playerRef.sendMessage(Message.raw("[C→S] " + name + " (ID: " + id + ")")); }

        return false;
    }
}