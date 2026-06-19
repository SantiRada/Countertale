package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;
import Tenzinn.Core.UI.CaseOpenPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.UUID;

public class OpenCaseCommand extends CommandBase {

    private final OptionalArg<String> skinArg;

    public OpenCaseCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
        skinArg = withOptionalArg("skin", "Skin id to force (e.g. --skin=ak47_crimson). Omit for random.", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        String skinId = skinArg.get(commandContext);
        CaseSkin skin;

        if (skinId != null && !skinId.isBlank()) {
            if (!playerRef.hasPermission("countertale.cases.admin")) {
                playerRef.sendMessage(Message.raw("[Cases] This option is only available to admins.").color(Color.RED));
                return;
            }

            // Debug path — bypass case requirement, use the specified skin
            skin = CaseManager.getSkinById(skinId);
            if (skin == null) {
                playerRef.sendMessage(Message.raw("[Cases] Unknown skin id: " + skinId).color(Color.RED));
                printSkinList(playerRef);
                return;
            }
        } else {
            // Normal path — require a case in inventory
            UUID uuid = playerRef.getUuid();
            if (CaseManager.getCaseCount(uuid) == 0) {
                playerRef.sendMessage(Message.raw("[Cases] You have no cases to open.").color(Color.RED));
                return;
            }
            CaseManager.useCase(uuid);
            skin = CaseManager.pickWinner();
        }

        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) { playerRef.sendMessage(Message.raw("[Cases] You must be in a world.").color(Color.RED)); return; }

        World world = Universe.get().getWorld(worldId);
        if (world == null) { playerRef.sendMessage(Message.raw("[Cases] World not found.").color(Color.RED)); return; }

        final CaseSkin finalSkin = skin;
        playerRef.sendMessage(Message.raw("[Cases] Opening roulette... winner pre-set: " + finalSkin.displayName).color(Color.GREEN));

        // openCustomPage must run on the WorldThread
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getPageManager().openCustomPage(ref, store, new CaseOpenPage(playerRef, finalSkin));
        });
    }

    private void printSkinList(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("[Cases] Available skin ids:").color(Color.YELLOW));
        for (CaseSkin s : CaseManager.SKINS) {
            playerRef.sendMessage(Message.raw("  - " + s.id + " (" + s.rarity.label + ")").color(Color.YELLOW));
        }
    }

    @Override
    public String getPermission() { return "countertale.case"; }
}
