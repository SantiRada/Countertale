package Tenzinn.Core.UI;

import Tenzinn.Core.Armory.ArmoryDetailPage;
import Tenzinn.Core.Armory.ArmoryPage;
import Tenzinn.Core.Cases.ArmorySkinAssets;
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
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class CaseInventoryPage extends InteractiveCustomUIPage<CaseInventoryPage.CaseInventoryEventData> {

    private static final int MAX_SKIN_SLOTS  = 30;
    private static final int MAX_CASE_SLOTS  = 6;

    private String filterRarity = "ALL";
    private String filterWeapon = "ALL";
    private final boolean returnToArmory;

    public CaseInventoryPage(PlayerRef playerRef) {
        this(playerRef, false);
    }

    public CaseInventoryPage(PlayerRef playerRef, boolean returnToArmory) {
        super(playerRef, CustomPageLifetime.CanDismiss, CaseInventoryEventData.CODEC);
        this.returnToArmory = returnToArmory;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Game/Cases/CaseInventory.ui");
        commandBuilder.set("#Username.TextSpans", Message.raw(playerRef.getUsername()));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HeaderClose",
                EventData.of("Action", "close"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "close"));

        for (int i = 1; i <= MAX_CASE_SLOTS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CaseSlot" + i,
                    EventData.of("Action", "open_case"));
        }
        for (int i = 1; i <= MAX_SKIN_SLOTS; i++) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#GridItem" + i,
                    EventData.of("Action", "skin:" + i));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#FrAll",
                EventData.of("Action", "filter_rarity:ALL"));
        for (CaseSkin.Rarity r : CaseSkin.Rarity.values()) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#" + rarityUiId(r),
                    EventData.of("Action", "filter_rarity:" + r.name()));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#FwAll",
                EventData.of("Action", "filter_weapon:ALL"));
        String[] weapons = {"Ak47","Awp","DesertEagle","DualBerettas","FiveSeven",
                            "M4a1s","Mac10","Mag7","Mp9","P250","SawedOff","Usp","Knife"};
        for (String w : weapons) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Fw" + w,
                    EventData.of("Action", "filter_weapon:" + w));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ClearFilters",
                EventData.of("Action", "clear"));

        populate(commandBuilder);
        sendUpdate();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull CaseInventoryEventData data) {
        String action = data.action != null ? data.action : "";

        if (action.equals("close")) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                if (returnToArmory) {
                    player.getPageManager().openCustomPage(ref, store, new ArmoryPage(playerRef));
                } else {
                    player.getPageManager().setPage(ref, store, Page.None);
                }
            }
            return;
        }

        if (action.equals("open_case")) {
            if (!CaseManager.useCase(playerRef.getUuid())) return;
            CaseSkin winner = CaseManager.pickWinner();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().openCustomPage(ref, store, new CaseOpenPage(playerRef, winner));
            }
            return;
        }

        if (action.startsWith("skin:")) {
            int slot = Integer.parseInt(action.substring("skin:".length())) - 1;
            List<CaseSkin> filtered = getFilteredSkins();
            if (slot >= 0 && slot < filtered.size()) {
                CaseSkin skin = filtered.get(slot);
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store,
                            new ArmoryDetailPage(playerRef, skin.weapon, skin.id));
                }
            }
            return;
        }

        if (action.equals("clear")) {
            filterRarity = "ALL";
            filterWeapon = "ALL";
        } else if (action.startsWith("filter_rarity:")) {
            filterRarity = action.substring("filter_rarity:".length());
        } else if (action.startsWith("filter_weapon:")) {
            filterWeapon = action.substring("filter_weapon:".length());
        }

        UICommandBuilder builder = new UICommandBuilder();
        populate(builder);
        sendUpdate(builder, false);
    }

    private void populate(UICommandBuilder b) {
        populateCases(b);
        populateGrid(b);
        populateFilterState(b);
    }

    private void populateCases(UICommandBuilder b) {
        int count = CaseManager.getCaseCount(playerRef.getUuid());
        boolean hasCases = count > 0;

        b.set("#CasesLabel.Visible", hasCases);
        b.set("#CasesRow.Visible",   hasCases);
        b.set("#CasesLabel.TextSpans", Message.raw("CASES (" + count + ")"));

        int shown = Math.min(count, MAX_CASE_SLOTS);
        for (int i = 1; i <= MAX_CASE_SLOTS; i++) {
            b.set("#CaseSlot" + i + ".Visible", i <= shown);
        }

        int extra = count - MAX_CASE_SLOTS;
        b.set("#MoreCasesLabel.Visible", extra > 0);
        if (extra > 0) b.set("#MoreCasesLabel.TextSpans", Message.raw("+ " + extra + " more"));
    }

    private void populateGrid(UICommandBuilder b) {
        List<CaseSkin> filtered = getFilteredSkins();
        int skinsTotal = CaseManager.getInventory(playerRef.getUuid()).size();
        int casesTotal = CaseManager.getCaseCount(playerRef.getUuid());
        int shown = Math.min(filtered.size(), MAX_SKIN_SLOTS);
        int visibleRows = (shown + 4) / 5;

        b.set("#InventoryCount.TextSpans",
                Message.raw(skinsTotal + " skin" + (skinsTotal == 1 ? "" : "s")
                        + " - " + casesTotal + " case" + (casesTotal == 1 ? "" : "s")));

        b.set("#EmptyLabel.Visible", shown == 0);
        b.set("#SkinsLabel.Visible", skinsTotal > 0 || !filterRarity.equals("ALL") || !filterWeapon.equals("ALL"));
        for (int row = 1; row <= 6; row++) {
            b.set("#SkinRow" + row + ".Visible", row <= visibleRows);
        }
        int contentHeight = 230
                + (casesTotal > 0 ? 180 : 0)
                + (shown > 0 ? 28 + (visibleRows * 136) : 42)
                + 90;
        b.set("#ScrollContent.ContentHeight", Math.max(500, contentHeight));

        for (int i = 1; i <= MAX_SKIN_SLOTS; i++) {
            if (i <= shown) {
                CaseSkin skin = filtered.get(i - 1);
                b.set("#GridItem"  + i + ".Visible",          true);
                b.set("#GridItem"  + i + ".OutlineColor",     skin.rarity.color);
                b.set("#GridIcon"  + i + ".Background",
                        Value.ref(ArmorySkinAssets.skinIconUiRef(), skin.iconUiKey));
                b.set("#GridName"  + i + ".TextSpans",        Message.raw(skin.displayName));
                b.set("#GridName"  + i + ".Style.TextColor",  skin.rarity.color);
                b.set("#GridRarity"+ i + ".TextSpans",        Message.raw(skin.rarity.label));
                b.set("#GridRarity"+ i + ".Style.TextColor",  skin.rarity.color);
            } else {
                b.set("#GridItem" + i + ".Visible", false);
            }
        }
    }

    private void populateFilterState(UICommandBuilder b) {
        b.set("#FrAll.OutlineSize", filterRarity.equals("ALL") ? 2 : 0);
        for (CaseSkin.Rarity r : CaseSkin.Rarity.values()) {
            b.set("#" + rarityUiId(r) + ".OutlineSize", filterRarity.equals(r.name()) ? 2 : 0);
        }

        b.set("#FwAll.OutlineSize", filterWeapon.equals("ALL") ? 2 : 0);
        String[] weapons = {"Ak47","Awp","DesertEagle","DualBerettas","FiveSeven",
                            "M4a1s","Mac10","Mag7","Mp9","P250","SawedOff","Usp","Knife"};
        for (String w : weapons) {
            b.set("#Fw" + w + ".OutlineSize", filterWeapon.equals(w) ? 2 : 0);
        }
    }

    private static String rarityUiId(CaseSkin.Rarity r) {
        return switch (r) {
            case MIL_SPEC   -> "FrMilSpec";
            case RESTRICTED -> "FrRestricted";
            case CLASSIFIED -> "FrClassified";
            case COVERT     -> "FrCovert";
            case SPECIAL    -> "FrSpecial";
        };
    }

    private List<CaseSkin> getFilteredSkins() {
        return CaseManager.getInventory(playerRef.getUuid()).stream()
                .filter(s -> filterRarity.equals("ALL") || s.rarity.name().equals(filterRarity))
                .filter(s -> filterWeapon.equals("ALL") || s.weapon.equals(filterWeapon))
                .collect(Collectors.toList());
    }

    public static class CaseInventoryEventData {
        public static final BuilderCodec<CaseInventoryEventData> CODEC = BuilderCodec
                .builder(CaseInventoryEventData.class, CaseInventoryEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (entry, s) -> entry.action = s,
                        (entry) -> entry.action)
                .add()
                .build();

        private String action;
    }
}
