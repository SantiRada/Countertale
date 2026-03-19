package Tenzinn.Core.UI;

import Tenzinn.Deathmatch.UI.MvpEventData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ModesPage extends InteractiveCustomUIPage<MvpEventData> {

    private UICommandBuilder uiBuilder;

    public ModesPage(PlayerRef playerRef) { super(playerRef, CustomPageLifetime.CanDismiss, Tenzinn.Deathmatch.UI.MvpEventData.CODEC); }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("Game/Modes.ui");
        uiBuilder = uiCommandBuilder;

        setListeners(uiEventBuilder);

        sendUpdate();
    }

    private void setListeners(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#dmButton", EventData.of("Action", "dm"));
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#fvfButton", EventData.of("Action", "fvf"));
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, MvpEventData data) {
        String action = data.getAction();

        Player player = store.getComponent(ref, Player.getComponentType());

        switch (action) {
            case "dm":
                CommandManager.get().handleCommand(playerRef, "queue --mode=dm");
                player.getPageManager().setPage(ref, store, Page.None);
            break;
            case "fvf":
                CommandManager.get().handleCommand(playerRef, "queue --mode=fvf");
                player.getPageManager().setPage(ref, store, Page.None);
            break;
        }

        sendUpdate();
    }
}