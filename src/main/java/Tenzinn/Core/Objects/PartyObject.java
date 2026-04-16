package Tenzinn.Core.Objects;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.PartyHUD;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PartyObject {

    public int id;
    public List<PlayerRef> players =  new ArrayList<PlayerRef>();
    public String leaderUsername;

    public PartyObject (int id, PlayerRef playerRef) {
        this.id = id;
        this.leaderUsername = playerRef.getUsername();
        this.players.add(playerRef);
    }
    // ================================================== //
    public void AddPlayer(PlayerRef playerRef) {
        for (int i = 0; i < players.size(); i++) { if (players.get(i).equals(playerRef)) return; }

        players.add(playerRef);

        UpdateHUD();

        SendMessageToAllPlayers(playerRef.getUsername() + " ha entrado al grupo.");
    }
    public void UpdateHUD() {
        for (int i = 0; i < players.size(); i++) {
            Player player = RefactorTool.getPlayer(players.get(i));

            CustomUIHud customHUD = player.getHudManager().getCustomHud();
            if(customHUD == null) {
                player.getHudManager().setCustomHud(players.get(i), new PartyHUD(players.get(i), this));
                return;
            }
            if(customHUD instanceof PartyHUD partyHUD) { partyHUD.setData(); }
        }

    }
    public void RemovePlayer(PlayerRef playerRef) { players.remove(playerRef); }
    public void TransferLeadership() {
        Objects.requireNonNull(Universe.get().getWorld(Objects.requireNonNull(players.getFirst().getWorldUuid()))).execute(() -> {
            Player player = RefactorTool.getPlayer(players.getFirst());
            player.getHudManager().setCustomHud(players.getFirst(), new PartyHUD(players.getFirst(), this));
        });
    }
    public void RemoveHUD(PlayerRef playerRef) {
        assert playerRef.getWorldUuid() != null;
        Objects.requireNonNull(Universe.get().getWorld(playerRef.getWorldUuid())).execute(() -> {
            Player player = RefactorTool.getPlayer(playerRef);
            if (player == null) return;
            player.getHudManager().resetHud(playerRef);
        });
    }
    // ================================================== //
    public void SendMessageToAllPlayers (String message) { for (PlayerRef player : players) { player.sendMessage(Message.raw(message)); } }
}