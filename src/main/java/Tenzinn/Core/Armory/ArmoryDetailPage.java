package Tenzinn.Core.Armory;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;
import Tenzinn.Core.Cases.ArmorySkinAssets;

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
import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArmoryDetailPage extends InteractiveCustomUIPage<ArmoryDetailPage.ArmoryDetailEventData> {

    private static final String WEAPONS_UI = "Game/images/weapons/Weapons.ui";
    private static final int    MAX_SLOTS  = 30;
    private static final String SELECTED_OUTLINE = "#F0B847";

    private static final java.util.Map<String, String> WEAPON_NAMES = new java.util.LinkedHashMap<>();
    static {
        WEAPON_NAMES.put("Knife",        "Knife");
        WEAPON_NAMES.put("Usp",          "USP-S");
        WEAPON_NAMES.put("DesertEagle",  "Desert Eagle");
        WEAPON_NAMES.put("FiveSeven",    "Five-SeveN");
        WEAPON_NAMES.put("P250",         "P250");
        WEAPON_NAMES.put("DualBerettas", "Dual Berettas");
        WEAPON_NAMES.put("Mac10",        "MAC-10");
        WEAPON_NAMES.put("Mp9",          "MP9");
        WEAPON_NAMES.put("Mag7",         "MAG-7");
        WEAPON_NAMES.put("SawedOff",     "Sawed-Off");
        WEAPON_NAMES.put("Ak47",         "AK-47");
        WEAPON_NAMES.put("M4a1s",        "M4A1-S");
        WEAPON_NAMES.put("Awp",          "AWP");
    }

    public static String getWeaponName(String weaponId) {
        return WEAPON_NAMES.getOrDefault(weaponId, weaponId);
    }

    private final String       weaponId;
    private final List<CaseSkin> weaponSkins;
    private final Set<String>    ownedIds;

    /** 0 = no skin; 1+ = weaponSkins index + 1 */
    private int selectedSlot = 0;

    public ArmoryDetailPage(PlayerRef playerRef, String weaponId) {
        this(playerRef, weaponId, null);
    }

    public ArmoryDetailPage(PlayerRef playerRef, String weaponId, String selectedSkinId) {
        super(playerRef, CustomPageLifetime.CanDismiss, ArmoryDetailEventData.CODEC);
        this.weaponId   = weaponId;
        this.weaponSkins = CaseManager.getSkinsByWeapon(weaponId);
        this.ownedIds   = CaseManager.getInventory(playerRef.getUuid())
                .stream().map(s -> s.id).collect(Collectors.toSet());

        String active = selectedSkinId != null
                ? selectedSkinId
                : CaseManager.getSelectedSkin(playerRef.getUuid(), weaponId);
        if (active != null) {
            for (int i = 0; i < weaponSkins.size(); i++) {
                if (weaponSkins.get(i).id.equals(active)) { selectedSlot = i + 1; break; }
            }
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder b,
                      @Nonnull UIEventBuilder ev, @Nonnull Store<EntityStore> store) {
        b.append("Game/Armory/ArmoryDetail.ui");

        b.set("#Username.TextSpans",    Message.raw(playerRef.getUsername()));
        b.set("#WeaponTitle.TextSpans", Message.raw(getWeaponName(weaponId).toUpperCase()));

        ev.addEventBinding(CustomUIEventBindingType.Activating, "#Close",
                EventData.of("Action", "close"));
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#Back",
                EventData.of("Action", "back"));
        ev.addEventBinding(CustomUIEventBindingType.Activating, "#SelectBtn",
                EventData.of("Action", "select"));

        for (int i = 1; i <= MAX_SLOTS; i++) {
            ev.addEventBinding(CustomUIEventBindingType.Activating, "#CarouselSlot" + i,
                    EventData.of("Action", "slot:" + i));
        }

        populate(b);
        sendUpdate();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ArmoryDetailEventData data) {
        String action = data.action != null ? data.action : "";
        Player player = store.getComponent(ref, Player.getComponentType());

        switch (action) {
            case "close" -> {
                if (player != null) player.getPageManager().setPage(ref, store, Page.None);
            }
            case "back" -> {
                if (player != null)
                    player.getPageManager().openCustomPage(ref, store, new ArmoryPage(playerRef));
            }
            case "select" -> handleSelect();
            default -> {
                if (action.startsWith("slot:")) {
                    int slot = Integer.parseInt(action.substring(5)) - 1; // to 0-based
                    if (slot >= 0 && slot <= weaponSkins.size()) {
                        selectedSlot = slot;
                        UICommandBuilder b = new UICommandBuilder();
                        populate(b);
                        sendUpdate(b, false);
                    }
                }
            }
        }
    }

    // ── Select logic ─────────────────────────────────────────────────────────

    private void handleSelect() {
        if (selectedSlot == 0) {
            CaseManager.clearSelectedSkin(playerRef.getUuid(), weaponId);
            playerRef.sendMessage(Message.raw("[Armería] Sin skin seleccionada para " + weaponId + ".")
                    .color(new java.awt.Color(0, 204, 85)));

            UICommandBuilder b = new UICommandBuilder();
            populate(b);
            sendUpdate(b, false);
            return;
        }

        int skinIndex = selectedSlot - 1;
        if (skinIndex >= weaponSkins.size()) return;
        CaseSkin skin = weaponSkins.get(skinIndex);

        // Anti-cheat: server-side ownership check
        if (!ownedIds.contains(skin.id)) {
            playerRef.sendMessage(Message.raw("[Armería] No tenés esa skin en tu inventario.")
                    .color(java.awt.Color.RED));
            return;
        }

        CaseManager.setSelectedSkin(playerRef.getUuid(), weaponId, skin.id);
        playerRef.sendMessage(Message.raw("[Armería] ✔ " + skin.displayName
                + " seleccionada para " + weaponId + ".")
                .color(new java.awt.Color(0, 204, 85)));

        UICommandBuilder b = new UICommandBuilder();
        populate(b);
        sendUpdate(b, false);
    }

    // ── Populate ─────────────────────────────────────────────────────────────

    private void populate(UICommandBuilder b) {
        int total = weaponSkins.size() + 1;
        b.set("#CarouselScroll.ContentWidth", Math.max(980, total * 140));

        for (int i = 0; i < MAX_SLOTS; i++) {
            int slotNum = i + 1; // 1-based id in .ui
            if (i < total) {
                boolean sel = (i == selectedSlot);

                b.set("#CarouselSlot" + slotNum + ".Visible",      true);
                b.set("#CarouselImg"  + slotNum + ".Background", Value.ref(WEAPONS_UI, weaponId + "on"));

                if (i == 0) {
                    b.set("#CarouselName" + slotNum + ".TextSpans", Message.raw("SIN SKIN"));
                    b.set("#CarouselName" + slotNum + ".Style.TextColor", "#bfcdd5");
                    b.set("#CarouselFrame" + slotNum + ".Background", sel ? "#1f2730" : "#111418");
                    b.set("#CarouselFrame" + slotNum + ".OutlineColor", sel ? SELECTED_OUTLINE : "#445160");
                    b.set("#CarouselFrame" + slotNum + ".OutlineSize", sel ? 4 : 1);
                    b.set("#CarouselLock" + slotNum + ".Visible", false);
                } else {
                    CaseSkin skin = weaponSkins.get(i - 1);
                    boolean owned = ownedIds.contains(skin.id);

                    b.set("#CarouselImg"  + slotNum + ".Background",
                            Value.ref(ArmorySkinAssets.skinIconUiRef(), skin.iconUiKey));
                    b.set("#CarouselName" + slotNum + ".TextSpans", Message.raw(skin.displayName));
                    b.set("#CarouselName" + slotNum + ".Style.TextColor", owned ? skin.rarity.color : "#657486");
                    b.set("#CarouselFrame" + slotNum + ".Background", sel ? "#1f2730" : "#111418");
                    b.set("#CarouselFrame" + slotNum + ".OutlineColor",
                            sel ? SELECTED_OUTLINE : (owned ? "#2b3542" : "#222222"));
                    b.set("#CarouselFrame" + slotNum + ".OutlineSize", sel ? 4 : 1);
                    b.set("#CarouselLock" + slotNum + ".Visible", !owned);
                }
            } else {
                b.set("#CarouselSlot" + slotNum + ".Visible", false);
            }
        }

        // Big preview + name + rarity + select button
        if (selectedSlot == 0) {
            b.set("#BigSkinImage.Background", Value.ref(WEAPONS_UI, weaponId + "on"));
            b.set("#SelectedSkinName.TextSpans", Message.raw("SIN SKIN"));
            b.set("#SelectedSkinName.Style.TextColor", "#FFFFFF");
            b.set("#SelectedSkinRarity.TextSpans", Message.raw("DEFAULT"));
            b.set("#SelectedSkinRarity.Style.TextColor", "#bfcdd5");
            b.set("#RarityBadge.OutlineColor", "#445160");
            b.set("#RarityBadge.OutlineSize", 1);
            b.set("#RarityBgMilSpec.Visible", false);
            b.set("#RarityBgRestricted.Visible", false);
            b.set("#RarityBgClassified.Visible", false);
            b.set("#RarityBgCovert.Visible", false);
            b.set("#RarityBgSpecial.Visible", false);
            b.set("#SelectBtn.Disabled", false);
        } else if (selectedSlot < total) {
            CaseSkin sel   = weaponSkins.get(selectedSlot - 1);
            boolean  owned = ownedIds.contains(sel.id);

            b.set("#BigSkinImage.Background",
                    Value.ref(ArmorySkinAssets.skinIconUiRef(), sel.iconUiKey));

            // Skin name: white if owned, readable gray if not
            b.set("#SelectedSkinName.TextSpans",       Message.raw(sel.displayName));
            b.set("#SelectedSkinName.Style.TextColor", owned ? "#FFFFFF" : "#96a9be");

            // Rarity badge: colored text + colored background at 20% opacity
            b.set("#SelectedSkinRarity.TextSpans",       Message.raw(sel.rarity.label.toUpperCase()));
            b.set("#SelectedSkinRarity.Style.TextColor", sel.rarity.color);
            b.set("#RarityBadge.OutlineColor",           sel.rarity.color);
            b.set("#RarityBadge.OutlineSize",            1);
            // Mostrar fondo de rareza correcto
            b.set("#RarityBgMilSpec.Visible",    sel.rarity == CaseSkin.Rarity.MIL_SPEC);
            b.set("#RarityBgRestricted.Visible", sel.rarity == CaseSkin.Rarity.RESTRICTED);
            b.set("#RarityBgClassified.Visible", sel.rarity == CaseSkin.Rarity.CLASSIFIED);
            b.set("#RarityBgCovert.Visible",     sel.rarity == CaseSkin.Rarity.COVERT);
            b.set("#RarityBgSpecial.Visible",    sel.rarity == CaseSkin.Rarity.SPECIAL);

            // Select Skin button: always visible, disabled when not owned
            b.set("#SelectBtn.Disabled", !owned);
        } else {
            b.set("#SelectedSkinName.TextSpans",   Message.raw("—"));
            b.set("#SelectedSkinRarity.TextSpans", Message.raw(""));
            b.set("#SelectBtn.Disabled", true);
        }
    }

    // ── EventData ─────────────────────────────────────────────────────────────

    public static class ArmoryDetailEventData {
        public static final BuilderCodec<ArmoryDetailEventData> CODEC = BuilderCodec
                .builder(ArmoryDetailEventData.class, ArmoryDetailEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (e, s) -> e.action = s,
                        (e)    -> e.action)
                .add()
                .build();

        private String action;
    }
}
