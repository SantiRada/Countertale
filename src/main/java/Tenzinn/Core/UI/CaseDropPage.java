package Tenzinn.Core.UI;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import javax.annotation.Nonnull;

public class CaseDropPage extends InteractiveCustomUIPage<CaseDropPage.CaseDropEventData> {

    public CaseDropPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CantClose, CaseDropEventData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Game/Cases/CaseDrop.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#OpenButton",
                EventData.of("Action", "open"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SkipButton",
                EventData.of("Action", "skip"));

        sendUpdate();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull CaseDropEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        switch (data.action) {
            case "open" -> {
                if (!CaseManager.useCase(playerRef.getUuid())) {
                    if (player != null) player.getPageManager().setPage(ref, store, Page.None);
                    CommandManager.get().handleCommand(playerRef, "lobby");
                    return;
                }
                CaseSkin winner = CaseManager.pickWinner();
                if (player != null) {
                    player.getPageManager().openCustomPage(ref, store, new CaseOpenPage(playerRef, winner));
                }
            }
            case "skip" -> {
                if (player != null) player.getPageManager().setPage(ref, store, Page.None);
                CommandManager.get().handleCommand(playerRef, "lobby");
            }
        }

        sendUpdate();
    }

    public static class CaseDropEventData {
        public static final BuilderCodec<CaseDropEventData> CODEC = BuilderCodec
                .builder(CaseDropEventData.class, CaseDropEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (entry, s) -> entry.action = s,
                        (entry) -> entry.action)
                .add()
                .build();

        private String action;
    }
}
