package Tenzinn.FiveVSfive.UI.Events;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.List;

public class ModesEventData {

    private String action;
    private String mode;
    private String[] selectedMaps;
    private String[] allMaps;

    public ModesEventData() { }

    public String getAction() { return action; }
    public String getMode() { return mode; }
    public String[] getSelectedMaps() { return selectedMaps; }
    public String[] getAllMaps() { return allMaps; }

    public void setMode(String mode) { this.mode = mode; }
    public void setSelectedMaps(List<String> maps) { this.selectedMaps = maps.toArray(new String[0]); }
    public void setAllMaps(List<String> maps) { this.allMaps = maps.toArray(new String[0]); }

    public static final BuilderCodec<ModesEventData> CODEC = BuilderCodec
            .builder(ModesEventData.class, ModesEventData::new).append(new KeyedCodec<>("Action", Codec.STRING),
                    (data, value) -> data.action = value, (data) -> data.action).add()
            .append(new KeyedCodec<>("Mode", Codec.STRING), (data, value) -> data.mode = value,
                    (data) -> data.mode).add() .append(new KeyedCodec<>("SelectedMaps", Codec.STRING_ARRAY),
                    (data, value) -> data.selectedMaps = value, (data) -> data.selectedMaps).add()
            .append(new KeyedCodec<>("AllMaps", Codec.STRING_ARRAY), (data, value) -> data.allMaps = value,
                    (data) -> data.allMaps).add().build();
}