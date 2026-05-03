package Tenzinn.FiveVSfive.Systems;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Localization.Lang;
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

        if (RefactorTool.getPlayerStats(sender) != null) {
            String content = event.getContent();
            event.setCancelled(true);

            List<PlayerRef> players = Objects.requireNonNull(RefactorTool.getPlayerStats(sender)).getCurrentMatch().getPlayers();
            int team = MatchFVF.validateTeamMembership(sender);

            for (int i = 0; i < players.size(); i++) {
                if (MatchFVF.validateTeamMembership(players.get(i)) == team) {
                    players.get(i).sendMessage(Lang.msg("chat.team", "player", sender.getUsername(), "message", content).color(Color.CYAN));
                }
            }
        }
    }
}
