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
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, ShopEventData data) {
        String action = data.getAction();
        int index = Integer.parseInt(action);

        WeaponStats newWeapon = RefactorTool.slots.get(index - 1);
        if (newWeapon.nameWeapon.equalsIgnoreCase("coming soon")) { RefactorTool.launchSound(playerRef, "fail"); }
        else { RefactorTool.launchSound(playerRef, "clic"); }

        if (mode == SpawnMode.DM) { RefactorTool.setLoot(playerRef, index); }
        else {
            // In FVF need verify available money
            PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

            if (playerStats.getMoney() >= newWeapon.pricing) {
                // Can buy
                if(playerStats.inShop) {
                    int pos = -1;
                    WeaponStats prevWeapon = null;

                    for (int i = 0; i < playerStats.getLoot().size(); i++) {
                        WeaponStats item = playerStats.getLoot().get(i);

                        if (newWeapon.typeWeapon.equalsIgnoreCase(item.typeWeapon)) {
                            prevWeapon = item;
                            pos = i;
                            break;
                        }
                    }

                    if(pos >= 0) {
                        playerStats.giveMoney(playerStats.moneySpent.get(pos) > 0 ? playerStats.moneySpent.get(pos) : 0); // Devolver dinero previo
                        playerRef.sendMessage(Message.raw("Changed " + prevWeapon.nameWeapon + " to " + newWeapon.nameWeapon));

                        playerStats.moneySpent.set(pos, newWeapon.pricing);
                    } else {
                        playerRef.sendMessage(Message.raw("El sistema no encontró el arma previa"));
                    }
                }

                playerStats.setMoney(newWeapon.pricing);
                playerStats.inShop = true;

                RefactorTool.setLoot(playerRef, index);

                uiBuilder.set("#UserMoney.TextSpans", Message.raw("$" + Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getMoney()));
                sendUpdate();
            }
            else { playerRef.sendMessage(Message.raw("Insufficient money")); }
        }

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
        for (int numberSlot = 1; numberSlot < (ShopData.getSizeNames()); numberSlot++) { uiBuilder.set("#Economic" + numberSlot + ".Visible", false); }
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