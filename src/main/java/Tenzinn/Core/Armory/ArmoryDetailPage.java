package Tenzinn.Core.Armory;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;

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
    private static final int    MAX_SLOTS  = 7;

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

    /** 0-based index of the slot currently highlighted in the carousel */
    private int selectedSlot = 0;

    public ArmoryDetailPage(PlayerRef playerRef, String weaponId) {
        super(playerRef, CustomPageLifetime.CantClose, ArmoryDetailEventData.CODEC);
        this.weaponId   = weaponId;
        this.weaponSkins = CaseManager.getSkinsByWeapon(weaponId);
        this.ownedIds   = CaseManager.getInventory(playerRef.getUuid())
                .stream().map(s -> s.id).collect(Collectors.toSet());

        // Pre-select the skin the player already has active (if any)
        String active = CaseManager.getSelectedSkin(playerRef.getUuid(), weaponId);
        if (active != null) {
            for (int i = 0; i < weaponSkins.size(); i++) {
                if (weaponSkins.get(i).id.equals(active)) { selectedSlot = i; break; }
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
                    if (slot >= 0 && slot < weaponSkins.size()) {
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
        if (selectedSlot >= weaponSkins.size()) return;
        CaseSkin skin = weaponSkins.get(selectedSlot);

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
        int total = weaponSkins.size();

        for (int i = 0; i < MAX_SLOTS; i++) {
            int slotNum = i + 1; // 1-based id in .ui
            if (i < total) {
                CaseSkin skin  = weaponSkins.get(i);
                boolean  owned = ownedIds.contains(skin.id);
                boolean  sel   = (i == selectedSlot);

                b.set("#CarouselSlot" + slotNum + ".Visible",      true);
                b.set("#CarouselImg"  + slotNum + ".Background",
                        Value.ref(WEAPONS_UI, skin.weapon + "on"));
                b.set("#CarouselSlot" + slotNum + ".OutlineColor",
                        sel ? skin.rarity.color : (owned ? "#444444" : "#222222"));
                b.set("#CarouselSlot" + slotNum + ".OutlineSize",  sel ? 2 : 1);
                b.set("#CarouselLock" + slotNum + ".Visible",      !owned);

                // Gray-out image for unowned skins via opacity
                // (Hytale UI doesn't support opacity on images directly, so we rely on the lock icon)
            } else {
                b.set("#CarouselSlot" + slotNum + ".Visible", false);
            }
        }

        // Big preview + name + rarity + select button
        if (!weaponSkins.isEmpty() && selectedSlot < total) {
            CaseSkin sel   = weaponSkins.get(selectedSlot);
            boolean  owned = ownedIds.contains(sel.id);

            b.set("#BigSkinImage.Background",  Value.ref(WEAPONS_UI, sel.weapon + "on"));

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
