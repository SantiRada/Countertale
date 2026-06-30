package Tenzinn.Core.Armory;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;
import Tenzinn.Core.UI.CaseInventoryPage;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArmoryPage extends InteractiveCustomUIPage<ArmoryPage.ArmoryEventData> {

    private static final String WEAPONS_UI = "Game/images/weapons/Weapons.ui";

    /** button id → weaponId */
    private static final Map<String, String> WEAPON_BUTTONS = new LinkedHashMap<>();
    static {
        WEAPON_BUTTONS.put("#WKnife",        "Knife");
        WEAPON_BUTTONS.put("#WUsp",          "Usp");
        WEAPON_BUTTONS.put("#WDesertEagle",  "DesertEagle");
        WEAPON_BUTTONS.put("#WFiveSeven",    "FiveSeven");
        WEAPON_BUTTONS.put("#WP250",         "P250");
        WEAPON_BUTTONS.put("#WDualBerettas", "DualBerettas");
        WEAPON_BUTTONS.put("#WMac10",        "Mac10");
        WEAPON_BUTTONS.put("#WMp9",          "Mp9");
        WEAPON_BUTTONS.put("#WMag7",         "Mag7");
        WEAPON_BUTTONS.put("#WSawedOff",     "SawedOff");
        WEAPON_BUTTONS.put("#WAk47",         "Ak47");
        WEAPON_BUTTONS.put("#WM4a1s",        "M4a1s");
        WEAPON_BUTTONS.put("#WAwp",          "Awp");
    }

    public ArmoryPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ArmoryEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder b,
                      @Nonnull UIEventBuilder ev, @Nonnull Store<EntityStore> store) {
        b.append("Game/Armory/Armory.ui");

        b.set("#Username.TextSpans", Message.raw(playerRef.getUsername()));

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#Close",
                EventData.of("Action", "close"));
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#ShowInventory",
                EventData.of("Action", "inventory"));

        for (Map.Entry<String, String> entry : WEAPON_BUTTONS.entrySet()) {
            ev.addEventBinding(CustomUIEventBindingType.Activating, entry.getKey(),
                    EventData.of("Action", "weapon:" + entry.getValue()));
        }

        populateIcons(b);
        populateSelected(b);
        sendUpdate();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ArmoryEventData data) {
        String action = data.action != null ? data.action : "";

        if ("close".equals(action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        if ("inventory".equals(action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) player.getPageManager().openCustomPage(ref, store, new CaseInventoryPage(playerRef, true));
            return;
        }

        if (action.startsWith("weapon:")) {
            String weaponId = action.substring(7);
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null)
                player.getPageManager().openCustomPage(ref, store, new ArmoryDetailPage(playerRef, weaponId));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void populateIcons(UICommandBuilder b) {
        for (Map.Entry<String, String> entry : WEAPON_BUTTONS.entrySet()) {
            String btnId   = entry.getKey();     // e.g. "#WAk47"
            String weapon  = entry.getValue();   // e.g. "Ak47"
            String iconId  = btnId + "Icon";    // e.g. "#WAk47Icon"
            b.set(iconId + ".Background", Value.ref(WEAPONS_UI, weapon + "on"));
        }
    }

    private void populateSelected(UICommandBuilder b) {
        for (Map.Entry<String, String> entry : WEAPON_BUTTONS.entrySet()) {
            String btnId  = entry.getKey();
            String weapon = entry.getValue();

            String selectedId = CaseManager.getSelectedSkin(playerRef.getUuid(), weapon);
            if (selectedId != null) {
                CaseSkin skin = CaseManager.getSkinById(selectedId);
                if (skin != null) {
                    b.set(btnId + ".OutlineColor", skin.rarity.color);
                    b.set(btnId + ".OutlineSize",  2);
                    continue;
                }
            }
            b.set(btnId + ".OutlineColor", "#2b3542");
            b.set(btnId + ".OutlineSize",  1);
        }
    }

    // ── EventData ─────────────────────────────────────────────────────────────

    public static class ArmoryEventData {
        public static final BuilderCodec<ArmoryEventData> CODEC = BuilderCodec
                .builder(ArmoryEventData.class, ArmoryEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (e, s) -> e.action = s,
                        (e)    -> e.action)
                .add()
                .build();

        private String action;
    }
}
