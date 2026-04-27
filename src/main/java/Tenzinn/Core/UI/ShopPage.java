package Tenzinn.Core.UI;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Shop.RevenuesConfig;
import Tenzinn.Core.Shop.ShopData;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Listeners.MapListeners.SpawnMode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
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

import java.util.Objects;

public class ShopPage extends InteractiveCustomUIPage<ShopEventData> {

    private UICommandBuilder uiBuilder;
    private SpawnMode mode;

    public ShopPage(PlayerRef playerRef, SpawnMode mode) {
        super(playerRef, CustomPageLifetime.CanDismiss, ShopEventData.CODEC);
        this.mode = mode;
    }

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

        if (mode == SpawnMode.FVF) { loadEconomy(); }
        else { hideEconomy(); }

        sendUpdate();
    }

    private void setListeners(UIEventBuilder uiEventBuilder) {
        for (int i = 1; i <= 25; i++) {
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Slot" + i, EventData.of("Action", String.valueOf(i)));
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref,
                                @NonNullDecl Store<EntityStore> store,
                                ShopEventData data) {
        if (data == null || data.getAction() == null) {
            RefactorTool.launchSound(playerRef, "fail");
            return;
        }

        String action = data.getAction();

        int index;
        try {
            index = Integer.parseInt(action);
        } catch (NumberFormatException e) {
            RefactorTool.launchSound(playerRef, "fail");
            return;
        }

        int slotIndex = index - 1;
        if (slotIndex < 0 || slotIndex >= RefactorTool.slots.size()) {
            RefactorTool.launchSound(playerRef, "fail");
            return;
        }

        WeaponStats newWeapon = RefactorTool.slots.get(slotIndex);
        if (newWeapon == null
                || newWeapon.nameWeapon == null
                || newWeapon.nameWeapon.equalsIgnoreCase("coming soon")
                || newWeapon.giveItems == null
                || newWeapon.giveItems.isEmpty()
                || newWeapon.pricing < 0) {
            RefactorTool.launchSound(playerRef, "fail");
            return;
        }

        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats == null || playerStats.getCurrentMatch() == null) {
            RefactorTool.launchSound(playerRef, "fail");
            return;
        }

        if (mode == SpawnMode.FVF) {
            if (!playerStats.getCurrentMatch().isBuyPhase() && !playerStats.canReceivedLoot) {
                playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_BUYING_LATE)));
                RefactorTool.launchSound(playerRef, "fail");
                return;
            }

            if (playerStats.getMoney() < newWeapon.pricing) {
                playerRef.sendMessage(Message.raw("Insufficient money"));
                RefactorTool.launchSound(playerRef, "fail");
                return;
            }

            if (playerStats.inShop) {
                int pos = -1;

                for (int i = 0; i < playerStats.getLoot().size(); i++) {
                    WeaponStats item = playerStats.getLoot().get(i);

                    if (item != null
                            && item.typeWeapon != null
                            && newWeapon.typeWeapon != null
                            && newWeapon.typeWeapon.equalsIgnoreCase(item.typeWeapon)) {
                        pos = i;
                        break;
                    }
                }

                if (pos >= 0 && pos < playerStats.moneySpent.size()) {
                    playerStats.giveMoney(playerStats.moneySpent.get(pos) > 0 ? playerStats.moneySpent.get(pos) : 0);
                    playerStats.moneySpent.set(pos, newWeapon.pricing);
                }
            }

            playerStats.setMoney(newWeapon.pricing);
            playerStats.inShop = true;
        }

        RefactorTool.setLoot(playerRef, index);
        RefactorTool.launchSound(playerRef, "clic");

        if (uiBuilder != null && mode == SpawnMode.FVF) {
            uiBuilder.set("#UserMoney.TextSpans", Message.raw("$" + playerStats.getMoney()));
            sendUpdate();
        }
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

        for (int numberSlot = 0; numberSlot < (ShopData.getSizeNames()); numberSlot++) {
            String number = ShopData.getNumbers(numberSlot);
            String image = ShopData.getImages(numberSlot);

            String name = ShopData.getNames(numberSlot);

            uiBuilder.set("#Slot" + (numberSlot + 1) + "Text.TextSpans", Message.raw(number));
            uiBuilder.set("#Slot" + (numberSlot + 1) + "Name.TextSpans", Message.raw(name));

            if(name.toLowerCase().contains("kevlar") || name.toLowerCase().contains("soon")) {
                uiBuilder.set("#Slot" + (numberSlot + 1) + "Icon.Background", Value.ref("Game/images/weapons/Weapons.ui", image));
            } else {
                uiBuilder.set("#Slot" + (numberSlot + 1) + "Icon.Background", Value.ref("Game/images/weapons/Weapons.ui", image + "active"));
            }
        }
    }

    private void hideEconomy() {
        for (int numberSlot = 1; numberSlot <= ShopData.getSizeNames(); numberSlot++) {
            uiBuilder.set("#Economic" + numberSlot + ".Visible", false);
        }
        uiBuilder.set("#Money.Visible", false);
        sendUpdate();
    }

    private void loadEconomy() {
        for (int numberSlot = 1; numberSlot <= (ShopData.getSizeNames()); numberSlot++) {
            if (ShopData.getPricing(numberSlot - 1) <= 0) { uiBuilder.set("#Economic" + numberSlot + ".Visible", false); }
            else { uiBuilder.set("#Price" + numberSlot + ".TextSpans", Message.raw("$" + ShopData.getPricing(numberSlot - 1))); }
        }

        uiBuilder.set("#UserMoney.TextSpans", Message.raw("$" + Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getMoney()));

        sendUpdate();
    }
}
