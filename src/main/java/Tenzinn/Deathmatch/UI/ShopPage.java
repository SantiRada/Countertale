package Tenzinn.Deathmatch.UI;

import Tenzinn.Deathmatch.Objects.WeaponStats;
import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.Shop.ShopData;
import Tenzinn.Listeners.MessageListeners;
import Tenzinn.Deathmatch.Objects.PlayerStats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

public class ShopPage extends InteractiveCustomUIPage<ShopEventData> {

    private UICommandBuilder uiBuilder;

    public ShopPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, Tenzinn.Deathmatch.UI.ShopEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Shop.ui");
        uiBuilder = uiCommandBuilder;

        RefactorTool.launchSound(playerRef, "clic");

        uiBuilder.set("#SelectLoot.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_SELECT_LOOT)));
        uiBuilder.set("#CloseShop.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_CLOSE_SHOP)));

        setListeners(uiEventBuilder);
        setTitleShop();
        loadContent();

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

        WeaponStats newWeapon = RefactorTool.slots.get(index - 1);
        if (newWeapon.nameWeapon.equalsIgnoreCase("coming soon")) { RefactorTool.launchSound(playerRef, "fail"); }
        else { RefactorTool.launchSound(playerRef, "clic"); }

        RefactorTool.setLoot(playerRef, index);

        sendUpdate();
    }

    private void setTitleShop() {
        if (uiBuilder == null) return;

        uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_NOT_BUYING_PHASE)));
        uiBuilder.set("#Timer.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_BUYING_LATE)));

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats == null) return;

        if (playerStats.getCurrentMatch().getState() == GameMatch.MatchState.WAITING) {
            uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_LOOT_START_GAME)));
            uiBuilder.set("#Timer.TextSpans", Message.raw(""));
        }

        if (playerStats.getCurrentMatch().isBuyPhase() || playerStats.canReceivedLoot) {
            uiBuilder.set("#DescriptionTimer.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_IN_BUYING_PHASE)));
            uiBuilder.set("#Timer.TextSpans", Message.raw(MessageListeners.get(MessageListeners.MessageKey.UI_WHEN_RECEIVES_LOOT)));
        }

        sendUpdate();
    }

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
}