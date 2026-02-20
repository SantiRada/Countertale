package Tenzinn.Deathmatch.UI;

import Tenzinn.Deathmatch.Objects.WeaponStats;

import Tenzinn.Tools.RefactorTool;
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ShopPage extends InteractiveCustomUIPage<ShopEventData> {

    private UICommandBuilder uiBuilder;
    private JsonObject shopData;

    public ShopPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, Tenzinn.Deathmatch.UI.ShopEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Shop.ui");
        uiBuilder = uiCommandBuilder;

        setListeners(uiEventBuilder);
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

        RefactorTool.setLoot(playerRef, index);

        sendUpdate();
    }

    private void loadContent() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/Common/UI/shop.json");

            if (inputStream == null) return;

            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            shopData = gson.fromJson(reader, JsonObject.class);

            reader.close();

            JsonArray categories = shopData.getAsJsonArray("categories");

            int numberCategory = 1;
            int numberSlot = 1;

            ArrayList<WeaponStats> slots = new ArrayList<>();

            for (JsonElement categoryElement : categories) {
                JsonObject category = categoryElement.getAsJsonObject();
                String categoryName = category.get("name").getAsString();
                JsonArray content = category.getAsJsonArray("content");

                uiBuilder.set("#Title0" + numberCategory + ".TextSpans", Message.raw(categoryName));

                for (JsonElement itemElement : content) {
                    JsonObject item = itemElement.getAsJsonObject();
                    String number = item.get("number").getAsString();
                    String image = item.get("image").getAsString();

                    String name = item.get("name").getAsString();
                    String typeWeapon = item.get("typeWeapon").getAsString();
                    String typeCross = item.get("typeCross").getAsString();
                    String firemode = item.get("firemode").getAsString();

                    ArrayList<String> giveContent = new ArrayList<>();
                    JsonArray giveItems = item.getAsJsonArray("id");
                    for(JsonElement itemSlot : giveItems) { giveContent.add(itemSlot.getAsString()); }

                    WeaponStats weapon = new WeaponStats(name, typeWeapon, typeCross, firemode, giveContent, image, numberSlot);
                    slots.add(weapon);

                    uiBuilder.set("#Slot" + numberSlot + "Text.TextSpans", Message.raw(number));
                    uiBuilder.set("#Slot" + numberSlot + "Name.TextSpans", Message.raw(name));

                    uiBuilder.set("#Slot" + numberSlot + "Icon.Background", Value.ref("Game/images/weapons/Weapons.ui", image));

                    numberSlot += 1;
                }

                numberCategory += 1;
            }

            RefactorTool.setSlots(slots);
        } catch (Exception e) {
            System.err.println("Error al cargar shop.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
}