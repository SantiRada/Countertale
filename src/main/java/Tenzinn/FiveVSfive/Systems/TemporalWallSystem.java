package Tenzinn.FiveVSfive.Systems;

import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MapListeners.TemporalWall;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.List;
import java.util.Objects;

public class TemporalWallSystem {

    public static void buildWalls(String mapName, PlayerRef playerRef) {
        List<TemporalWall> walls = MapListeners.getWalls(mapName);

        World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
        if (currentWorld == null) return;

        currentWorld.execute(() -> {
            for (TemporalWall wall : walls) {
                int fromX = (int) wall.fromX, toX = (int) wall.toX;
                int fromY = (int) wall.fromY, toY = (int) wall.toY;
                int fromZ = (int) wall.fromZ, toZ = (int) wall.toZ;

                int stepX = fromX <= toX ? 1 : -1;
                int stepY = fromY <= toY ? 1 : -1;
                int stepZ = fromZ <= toZ ? 1 : -1;

                for (int x = fromX; x != toX + stepX; x += stepX) {
                    for (int y = fromY; y != toY + stepY; y += stepY) {
                        for (int z = fromZ; z != toZ + stepZ; z += stepZ) {
                            Vector3i pos = new Vector3i(x, y, z);
                            if (Objects.requireNonNull(currentWorld.getBlockType(pos)).getId().equalsIgnoreCase("empty")) {
                                currentWorld.setBlock(x, y, z, "Rock_Stone_Cobble");
                            }
                        }
                    }
                }
            }
        });
    }
    public static void removeWalls(String mapName, PlayerRef playerRef) {
        List<TemporalWall> walls = MapListeners.getWalls(mapName);

        assert playerRef.getWorldUuid() != null;
        World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
        if (currentWorld == null) return;

        currentWorld.execute(() -> {
            for (TemporalWall wall : walls) {
                int fromX = (int) wall.fromX, toX = (int) wall.toX;
                int fromY = (int) wall.fromY, toY = (int) wall.toY;
                int fromZ = (int) wall.fromZ, toZ = (int) wall.toZ;

                int stepX = fromX <= toX ? 1 : -1;
                int stepY = fromY <= toY ? 1 : -1;
                int stepZ = fromZ <= toZ ? 1 : -1;

                for (int x = fromX; x != toX + stepX; x += stepX) {
                    for (int y = fromY; y != toY + stepY; y += stepY) {
                        for (int z = fromZ; z != toZ + stepZ; z += stepZ) {
                            Vector3i pos = new Vector3i(x, y, z);
                            if (Objects.requireNonNull(currentWorld.getBlockType(pos)).getId().equalsIgnoreCase("Rock_Stone_Cobble")) {
                                currentWorld.setBlock(x, y, z, "Empty");
                            }
                        }
                    }
                }
            }
        });
    }
}