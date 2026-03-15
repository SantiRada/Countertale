package Tenzinn.Deathmatch.Content.UI;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class MvpEventData {

    private String action;

    public MvpEventData() { }

    public String getAction() { return action; }

    public static final BuilderCodec<MvpEventData> CODEC = BuilderCodec
            .builder(MvpEventData.class, MvpEventData::new).append(new KeyedCodec<>("Action", Codec.STRING), (data, value) -> data.action = value,
                    (data) -> data.action).add().build();
}