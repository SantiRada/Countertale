package Tenzinn.Deathmatch.Content.UI;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ShopEventData {

    private String action;

    public ShopEventData() {}

    public String getAction() { return action; }

    public static final BuilderCodec<ShopEventData> CODEC = BuilderCodec
            .builder(ShopEventData.class, ShopEventData::new).append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value,
                    (data) -> data.action).add().build();
}