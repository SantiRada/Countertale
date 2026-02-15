package Tenzinn.Handle;

import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;

import javax.annotation.Nonnull;
import java.awt.*;

public class DeathDetector implements PlayerPacketFilter {

    private long lastDeathTime = 0;
    private static final long COOLDOWN_MS = 2000;
    private int countCalls = 0;

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (packet.getId() == 219) {
            long currentTime = System.currentTimeMillis();
            if (countCalls == 1) {
                if (currentTime - lastDeathTime > COOLDOWN_MS) {
                    RefactorTool.Respawn(playerRef);
                    lastDeathTime = currentTime;
                    countCalls = 0;
                }
            } else { if (currentTime - lastDeathTime > COOLDOWN_MS) { countCalls = 1; } }
        }

        return false;
    }
}