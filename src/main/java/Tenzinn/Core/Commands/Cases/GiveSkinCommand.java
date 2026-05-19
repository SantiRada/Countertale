package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.Cases.CaseSkin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class GiveSkinCommand extends CommandBase {

    private final OptionalArg<String> skinArg;

    public GiveSkinCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
        skinArg = withOptionalArg("skin", "Skin id to give (e.g. --skin=ak47_crimson). Omit for random.", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        String skinId = skinArg.get(commandContext);
        CaseSkin skin;

        if (skinId != null && !skinId.isBlank()) {
            skin = CaseManager.getSkinById(skinId);
            if (skin == null) {
                playerRef.sendMessage(Message.raw("[Cases] Unknown skin id: " + skinId).color(Color.RED));
                playerRef.sendMessage(Message.raw("[Cases] Use /case inventory to see available ids.").color(Color.YELLOW));
                return;
            }
        } else {
            skin = CaseManager.pickWinner();
        }

        CaseManager.addToInventory(playerRef.getUuid(), skin);
        playerRef.sendMessage(Message.raw(
                "[Cases] Added to inventory: " + skin.displayName + " [" + skin.rarity.label + "]"
        ).color(Color.GREEN));
    }
}
