package Tenzinn.Deathmatch.UI;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ShopPage extends CustomUIPage {

    private UICommandBuilder uiBuilder;
    private JsonObject shopData;

    public ShopPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Shop.ui");
        uiBuilder = uiCommandBuilder;

        loadContent();
        sendUpdate();
    }

    private void loadContent() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/Common/UI/shop.json");

            if (inputStream == null) {
                System.err.println("No se pudo encontrar /Common/UI/shop.json");
                return;
            }

            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            shopData = gson.fromJson(reader, JsonObject.class);

            reader.close();

            JsonArray categories = shopData.getAsJsonArray("categories");

            int numberCategory = 1;
            int numberSlot = 1;

            for (JsonElement categoryElement : categories) {
                JsonObject category = categoryElement.getAsJsonObject();
                String categoryName = category.get("name").getAsString();
                JsonArray content = category.getAsJsonArray("content");

                uiBuilder.set("#Title0" + numberCategory + ".TextSpans", Message.raw(categoryName));

                for (JsonElement itemElement : content) {
                    JsonObject item = itemElement.getAsJsonObject();
                    String number = item.get("number").getAsString();
                    String name = item.get("name").getAsString();
                    String image = item.get("image").getAsString();

                    uiBuilder.set("#Slot" + numberSlot + "Text.TextSpans", Message.raw(number));
                    uiBuilder.set("#Slot" + numberSlot + "Name.TextSpans", Message.raw(name));

                    uiBuilder.set("#Slot" + numberSlot + "Icon.Background", Value.ref("Game/images/weapons/Weapons.ui", image));

                    numberSlot += 1;
                }

                numberCategory += 1;
            }

        } catch (Exception e) {
            System.err.println("Error al cargar shop.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
}