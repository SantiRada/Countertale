package Tenzinn.Core.UI;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaseOpenPage extends InteractiveCustomUIPage<CaseOpenPage.CaseOpenEventData> {

    private static final long[] TICK_DELAYS = {
            50, 50, 50, 50, 50,
            75, 75, 75, 75,
            100, 100, 100,
            150, 150, 200,
            300, 500, 800, 1200
    };

    private static final String WEAPONS_UI_REF = "Game/images/weapons/Weapons.ui";

    private final CaseSkin winner;
    private List<CaseSkin> rouletteStrip;
    private ScheduledFuture<?> lastScheduled;
    private final AtomicBoolean animationDone = new AtomicBoolean(false);

    public CaseOpenPage(PlayerRef playerRef, CaseSkin winner) {
        super(playerRef, CustomPageLifetime.CantClose, CaseOpenEventData.CODEC);
        this.winner = winner;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Game/Cases/CaseOpen.ui");

        rouletteStrip = CaseManager.generateRoulette(winner);

        applySlots(commandBuilder, 0, false);

        commandBuilder.set("#CollectButton.Disabled", true);
        commandBuilder.set("#WinnerName.TextSpans",   Message.raw(""));
        commandBuilder.set("#WinnerRarity.TextSpans", Message.raw(""));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CollectButton",
                EventData.of("Action", "collect"));

        sendUpdate();
        startAnimation();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull CaseOpenEventData data) {
        if ("collect".equals(data.action) && animationDone.get()) {
            if (lastScheduled != null && !lastScheduled.isDone()) lastScheduled.cancel(false);

            CaseManager.addToInventory(playerRef.getUuid(), winner);

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) player.getPageManager().setPage(ref, store, Page.None);

            CommandManager.get().handleCommand(playerRef, "lobby");
        }
    }

    private void startAnimation() {
        long cumulativeMs = 0;
        for (int frame = 0; frame < TICK_DELAYS.length; frame++) {
            cumulativeMs += TICK_DELAYS[frame];
            final int nextOffset = frame + 1;
            final boolean isLast = (frame == TICK_DELAYS.length - 1);
            final long delay = cumulativeMs;

            ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                UUID worldId = playerRef.getWorldUuid();
                if (worldId == null) return;
                World world = Universe.get().getWorld(worldId);
                if (world == null) return;
                world.execute(() -> {
                    UICommandBuilder tick = new UICommandBuilder();
                    applySlots(tick, nextOffset, isLast);

                    if (isLast) {
                        animationDone.set(true);
                        revealWinner(tick);
                    }

                    RefactorTool.launchSound(playerRef, "clic");
                    sendUpdate(tick, false);
                });
            }, delay, TimeUnit.MILLISECONDS);

            if (isLast) lastScheduled = task;
        }
    }

    private void applySlots(UICommandBuilder builder, int offset, boolean isWinnerFrame) {
        for (int i = 0; i < 7; i++) {
            CaseSkin skin = rouletteStrip.get(offset + i);
            int slot = i + 1;

            builder.set("#Slot" + slot + "Icon.Background",
                    Value.ref(WEAPONS_UI_REF, skin.weapon + "on"));
            builder.set("#Slot" + slot + "Name.TextSpans",
                    Message.raw(skin.displayName));
            builder.set("#Slot" + slot + ".Background", slotBgColor(skin.rarity));
            builder.set("#Slot" + slot + ".OutlineColor", skin.rarity.color);

            boolean isCenter = (i == 3);
            if (isCenter && isWinnerFrame) {
                builder.set("#Slot4.OutlineSize", 4);
                builder.set("#Slot4Name.Style.TextColor", winner.rarity.color);
            } else {
                builder.set("#Slot" + slot + ".OutlineSize", isCenter ? 2 : 1);
            }
        }
    }

    private void revealWinner(UICommandBuilder builder) {
        builder.set("#WinnerIcon.Background",
                Value.ref(WEAPONS_UI_REF, winner.weapon + "on"));
        builder.set("#WinnerIconContainer.OutlineColor", winner.rarity.color);
        builder.set("#WinnerIconContainer.OutlineSize",  3);
        builder.set("#WinnerIconContainer.Background",   slotBgColor(winner.rarity));

        builder.set("#WinnerRarity.TextSpans",
                Message.raw(winner.rarity.label));
        builder.set("#WinnerRarity.Style.TextColor", winner.rarity.color);
        builder.set("#WinnerName.TextSpans",
                Message.raw(winner.displayName));
        builder.set("#WinnerName.Style.TextColor", winner.rarity.color);

        builder.set("#CollectButton.Disabled", false);
    }

    private static String slotBgColor(CaseSkin.Rarity rarity) {
        return switch (rarity) {
            case MIL_SPEC   -> "#0C2224";
            case RESTRICTED -> "#110020";
            case CLASSIFIED -> "#22001E";
            case COVERT     -> "#220000";
            case SPECIAL    -> "#221A00";
        };
    }

    public static class CaseOpenEventData {
        public static final BuilderCodec<CaseOpenEventData> CODEC = BuilderCodec
                .builder(CaseOpenEventData.class, CaseOpenEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (entry, s) -> entry.action = s,
                        (entry) -> entry.action)
                .add()
                .build();

        private String action;
    }
}
