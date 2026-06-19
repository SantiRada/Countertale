package Tenzinn.Core.Objects;

import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.PartyHUD;
import Tenzinn.Core.UI.QueueHud;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PartyObject {

    public int id;
    public List<PlayerRef> players = new ArrayList<>();
    public String leaderUsername;

    public PartyObject(int id, PlayerRef playerRef) {
        this.id = id;
        this.leaderUsername = playerRef.getUsername();
        this.players.add(playerRef);
    }

    public void AddPlayer(PlayerRef playerRef) {
        for (PlayerRef ref : players) if (ref.equals(playerRef)) return;

        if (players.size() >= 5) {
            playerRef.sendMessage(Lang.msg("party.full").color(Color.red));
            return;
        }

        players.add(playerRef);
        SendMessageToAllPlayers(Lang.msg("party.member-joined", "player", playerRef.getUsername()));

        if (playerRef.getWorldUuid() != null) {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world != null) world.execute(this::UpdateHUD);
        }
    }

    public void UpdateHUD() {
        for (PlayerRef playerRef : players) {
            Player player = RefactorTool.getPlayer(playerRef);
            if (player == null) continue;

            CustomUIHud customHUD = player.getHudManager().getCustomHud(playerRef.getUuid().toString());
            if (customHUD instanceof QueueHud queueHUD) {
                queueHUD.setDataParty(this);
            } else {
                player.getHudManager().addCustomHud(playerRef, new PartyHUD(playerRef, this));
            }
        }
    }

    public void RemovePlayer(PlayerRef playerRef) {
        players.remove(playerRef);

        assert playerRef.getWorldUuid() != null;
        World world = Universe.get().getWorld(playerRef.getWorldUuid());

        assert world != null;
        world.execute(() -> {
            Player player = RefactorTool.getPlayer(playerRef);
            CustomUIHud customHud = player.getHudManager().getCustomHud(playerRef.getUuid().toString());
            if (customHud instanceof QueueHud queueHUD) queueHUD.setDeleteParty();
            if (customHud instanceof PartyHUD) player.getHudManager().removeCustomHud(playerRef, playerRef.getUuid().toString());
        });
    }

    public void TransferLeadership() {
        if (players.isEmpty()) return;
        if (players.getFirst().getWorldUuid() == null) return;
        World world = Universe.get().getWorld(players.getFirst().getWorldUuid());
        if (world == null) return;
        world.execute(this::UpdateHUD);
    }

    public void RemoveHUD(PlayerRef playerRef) {
        assert playerRef.getWorldUuid() != null;
        Objects.requireNonNull(Universe.get().getWorld(playerRef.getWorldUuid())).execute(() -> {
            Player player = RefactorTool.getPlayer(playerRef);
            if (player == null) return;
            player.getHudManager().resetHud(playerRef);
        });
    }

    public void SendMessageToAllPlayers(Message message) {
        for (PlayerRef player : players) player.sendMessage(message);
    }
}
