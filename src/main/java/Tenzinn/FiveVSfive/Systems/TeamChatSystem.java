package Tenzinn.FiveVSfive.Systems;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class TeamChatSystem {
    public static void onPlayerChat(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();

        var senderStats = RefactorTool.getPlayerStats(sender);
        if (senderStats == null || senderStats.getCurrentMatch() == null) return;

        if (!senderStats.getCurrentMatch().getMode().equalsIgnoreCase("fvf")) {
            return;
        }

        String content = event.getContent();
        event.setCancelled(true);

        List<PlayerRef> players = senderStats.getCurrentMatch().getPlayers();
        int team = MatchFVF.validateTeamMembership(sender);

        for (PlayerRef player : players) {
            if (MatchFVF.validateTeamMembership(player) == team) {
                player.sendMessage(Message.raw("[TEAM] " + sender.getUsername() + ": " + content).color(Color.CYAN));
            }
        }
    }
}