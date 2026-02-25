package Tenzinn.Deathmatch.Shop;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ShopData {

    private static JsonObject shopData;

    private static ArrayList<String> titles = new ArrayList<>();
    private static ArrayList<String> numbers = new ArrayList<>();
    private static ArrayList<String> images = new ArrayList<>();
    private static ArrayList<String> names = new ArrayList<>();

    public static void loadContent() {
        if(titles.size() > 2) return;

        try {
            InputStream inputStream = ShopData.class.getResourceAsStream("/Common/UI/shop.json");

            System.out.println("SHOPDATA.JAVA: Cargando data de Shop.json");

            if (inputStream == null) return;

            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            shopData = gson.fromJson(reader, JsonObject.class);

            reader.close();

            JsonArray categories = shopData.getAsJsonArray("categories");

            ArrayList<WeaponStats> slots = new ArrayList<>();
            int numberSlot = 1;

            for (JsonElement categoryElement : categories) {
                JsonObject category = categoryElement.getAsJsonObject();
                String categoryName = category.get("name").getAsString();
                JsonArray content = category.getAsJsonArray("content");

                titles.add(categoryName);

                for (JsonElement itemElement : content) {
                    JsonObject item = itemElement.getAsJsonObject();
                    numbers.add(item.get("number").getAsString());
                    images.add(item.get("image").getAsString());

                    names.add(item.get("name").getAsString());

                    ArrayList<String> gives = new ArrayList<>();
                    JsonArray giveItems = item.getAsJsonArray("id");
                    for(JsonElement itemSlot : giveItems) { gives.add(itemSlot.getAsString()); }

                    WeaponStats weapon = new WeaponStats(
                            names.getLast(),
                            item.get("typeWeapon").getAsString(),
                            item.get("typeCross").getAsString(),
                            item.get("firemode").getAsString(),
                            gives,
                            images.getLast(), numberSlot
                    );
                    slots.add(weapon);

                    numberSlot += 1;
                }
            }

            RefactorTool.setSlots(slots);

            System.out.println("SHOPDATA.JAVA: Información cargada en Shop.json");
        } catch (Exception e) {
            System.err.println("Error al cargar shop.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getNumbers (int index) { return numbers.size() > index ? numbers.get(index) : "NaN"; }
    public static String getImages (int index) { return images.size() > index ? images.get(index) : "NaN"; }
    public static String getNames (int index) { return names.size() > index ? names.get(index) : "NaN"; }

    public static ArrayList<String> getArrayTitle () { return titles; }
    public static int getSizeNames () { return names.isEmpty() ? -1 : names.size(); }
}
