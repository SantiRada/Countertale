package Tenzinn.Core.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import com.hypixel.hytale.codec.Codec;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector2d;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.common.util.CompletableFutureUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DiePage extends InteractiveCustomUIPage<DiePage.DiePageEventData> {

    public ScheduledFuture<?> timerTask;
    private int timeToRespawn;

    public DiePage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CantClose, DiePageEventData.CODEC); }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        boolean isFvF = RefactorTool.getModeForPlayer(playerRef) == MapListeners.SpawnMode.FVF;

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RespawnButton", EventData.of("Action", "Respawn"));

        if (isFvF) {
            commandBuilder.append("Game/DiePage_FVF.ui");
            commandBuilder.set("#TeamAlive.TextSpans", Message.raw(String.valueOf(countAliveTeammates())));
            commandBuilder.set("#RespawnButton.Disabled", false);
        } else {
            commandBuilder.append("Game/DiePage.ui");

            timeToRespawn = 10;
            commandBuilder.set("#RespawnButton.Disabled", true);

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                timeToRespawn -= 1;

                UICommandBuilder tickBuilder = new UICommandBuilder();

                int minutes = timeToRespawn / 60;
                int seconds = timeToRespawn % 60;
                tickBuilder.set("#Timer.TextSpans", Message.raw(String.format("%02d:%02d", minutes, seconds)));

                if (timeToRespawn <= 0) {
                    timerTask.cancel(false);
                    timerTask = null;
                    tickBuilder.set("#Timer.Visible", false);
                    tickBuilder.set("#RespawnButton.Disabled", false);
                }

                sendUpdate(tickBuilder, false);
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    private int countAliveTeammates() {
        int myTeam = MatchFVF.validateTeamMembership(playerRef);
        if (myTeam < 0) return 0;

        int count = 0;
        for (PlayerRef p : MatchFVF.getPlayers()) {
            if (p.equals(playerRef)) continue;
            if (MatchFVF.validateTeamMembership(p) != myTeam) continue;
            PlayerStats ps = RefactorTool.getPlayerStats(p);
            if (ps != null && ps.playerState == PlayerStats.PlayerState.DEFAULT) count++;
        }
        return count;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull DiePageEventData data) {
        if ("Respawn".equals(data.action)) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            commandBuilder.set("#RespawnButton.Disabled", true);
            sendUpdate(commandBuilder, false);

            CompletableFutureUtil._catch(DeathComponent.respawn(store, ref).thenCompose((v) -> {
                Ref<EntityStore> currentRef = playerRef.getReference();

                if (currentRef != null && currentRef.isValid()) {
                    return CompletableFuture.runAsync(() -> {
                        if (currentRef.isValid()) {
                            Player playerComponent = currentRef.getStore().getComponent(currentRef, Player.getComponentType());
                            if (playerComponent != null && playerComponent.getPageManager().getCustomPage() == this) {
                                timerTask.cancel(false);
                                timerTask = null;
                                playerComponent.getPageManager().setPage(currentRef, currentRef.getStore(), Page.None);
                            }
                        }
                    }, currentRef.getStore().getExternalData().getWorld());
                }
                return CompletableFuture.completedFuture(null);})
            );

            World newWorld = Universe.get().getWorld(playerRef.getWorldUuid());

            Vector3d spawn;
            if (RefactorTool.getModeForPlayer(playerRef).equals(MapListeners.SpawnMode.DM)) {
                spawn = RefactorTool.getRandomSpawn(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch().getMapId());
            } else {
                spawn = RefactorTool.getSpawnForTeam(playerRef);
            }

            World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
            Ref<EntityStore> ref2 = playerRef.getReference();
            currentWorld.execute(() -> {
                try {
                    Store<EntityStore> storeWorld = currentWorld.getEntityStore().getStore();
                    Teleport teleport = Teleport.createForPlayer(newWorld, new Transform(spawn.x, spawn.y, spawn.z));
                    storeWorld.addComponent(ref2, Teleport.getComponentType(), teleport);
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

    public static class DiePageEventData {
        public static final BuilderCodec CODEC = BuilderCodec
                .builder(DiePageEventData.class, DiePageEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (entry, s) -> entry.action = s,
                        (entry) -> entry.action)
                .add()
                .build();

        private String action;
    }
}