package Tenzinn.Deathmatch.UI;

import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.Objects.PlayerStats;
import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Shop.ShopData;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ShopPage extends InteractiveCustomUIPage<ShopEventData> {

    private UICommandBuilder uiBuilder;

    public ScheduledFuture<?> timerTask;
    private int remainingSeconds = 15;

    public ShopPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, Tenzinn.Deathmatch.UI.ShopEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Shop.ui");
        uiBuilder = uiCommandBuilder;

        setListeners(uiEventBuilder);
        loadContent();
        setTimer();

        sendUpdate();
    }

    private void setListeners(UIEventBuilder uiEventBuilder) {
        for (int i = 1; i <= 25; i++) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Slot" + i, EventData.of("Action", String.valueOf(i)));
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, ShopEventData data) {
        String action = data.getAction();
        int index = Integer.parseInt(action);

        RefactorTool.setLoot(playerRef, index);

        sendUpdate();
    }

    private void setTimer() {
        if (uiBuilder == null) return;

        uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw("You're not in the buying phase."));
        uiBuilder.set("#Timer.TextSpans", Message.raw("Loot on revive."));

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if(playerStats == null) return;

        if (playerStats.getCurrentMatch().getState() == GameMatch.MatchState.WAITING) {
            uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw("The loot will be added at the start of the game."));
            uiBuilder.set("#Timer.TextSpans", Message.raw(""));
            return;
        }

        if (playerStats.getCurrentMatch().getState() != GameMatch.MatchState.STARTING && !playerStats.canReceivedLoot) return;

        uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw("Time remaining to buy"));

        remainingSeconds = playerStats.getCurrentMatch().getTimer();

        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
                if (stats == null || stats.getCurrentMatch() == null) {
                    if (timerTask != null) timerTask.cancel(false);
                    return;
                }

                remainingSeconds = stats.getCurrentMatch().getTimer();
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timerText = String.format("%02d:%02d", minutes, seconds);

                uiBuilder.set("#Timer.TextSpans", Message.raw(timerText));

                if (remainingSeconds <= 0) { stopTimer(); }

            } catch (Exception e) { if (timerTask != null) timerTask.cancel(false); }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void stopTimer () { if (timerTask != null && !timerTask.isDone()) { timerTask.cancel(false); timerTask = null; } }

    private void loadContent() {
        int numberCategory = 1;
        for (String categoryName : ShopData.getArrayTitle()) {
            uiBuilder.set("#Title0" + numberCategory + ".TextSpans", Message.raw(categoryName));

            numberCategory += 1;
        }

        for (int numberSlot = 0; numberSlot < (ShopData.getSizeNames() - 1); numberSlot++) {
            String number = ShopData.getNumbers(numberSlot);
            String image = ShopData.getImages(numberSlot);

            String name = ShopData.getNames(numberSlot);

            uiBuilder.set("#Slot" + (numberSlot + 1) + "Text.TextSpans", Message.raw(number));
            uiBuilder.set("#Slot" + (numberSlot + 1) + "Name.TextSpans", Message.raw(name));
            uiBuilder.set("#Slot" + (numberSlot + 1) + "Icon.Background", Value.ref("Game/images/weapons/Weapons.ui", image));
        }
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) { stopTimer(); }
}