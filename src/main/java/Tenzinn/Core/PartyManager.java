package Tenzinn.Core;

import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Objects.InvitationParty;
import Tenzinn.Core.Objects.PartyObject;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class PartyManager {

    public static List<PartyObject> totalParty = new CopyOnWriteArrayList<>();
    public static List<InvitationParty> totalInvitations = new CopyOnWriteArrayList<>();
    private static int partyIdCounter = 0;

    public static void CreateParty(PlayerRef playerRef) {
        for (PartyObject partyObject : totalParty) {
            if (partyObject.players.stream().anyMatch(player -> player == playerRef)) {
                playerRef.sendMessage(Lang.msg("party.already-in-party"));
                return;
            }
        }

        PartyObject newParty = new PartyObject(partyIdCounter++, playerRef);
        totalParty.add(newParty);

        HytaleServer.SCHEDULED_EXECUTOR.schedule(newParty::TransferLeadership, 100, TimeUnit.MILLISECONDS);
        playerRef.sendMessage(Lang.msg("party.created").color(Color.cyan));
    }

    public static void InviteToParty(int id, PlayerRef playerRef) {
        String leader = GetLeaderById(id);
        if (leader == null) return;

        int partyIndex = GetIndexPartyById(id);
        if (partyIndex >= 0 && totalParty.get(partyIndex).players.size() >= 5) {
            SendMessageToLeader(id, Lang.msg("party.full"));
            return;
        }

        int testInvitation = GetInvitationByPlayer(playerRef);
        if (testInvitation >= 0) {
            playerRef.sendMessage(Lang.msg("party.invite-pending").color(Color.orange));
            return;
        }

        SendMessageToLeader(id, Lang.msg("party.invite-sent", "player", playerRef.getUsername()));
        playerRef.sendMessage(Lang.msg("party.invited", "leader", leader).color(Color.cyan));

        InvitationParty newIntivation = new InvitationParty(id, playerRef);
        totalInvitations.add(newIntivation);
    }

    public static void JoinToParty(PlayerRef playerRef) {
        int index = ValidateInvitationToPlayer(playerRef);
        if (index < 0) return;

        int indexParty = GetIndexPartyById(totalInvitations.get(index).id);
        if (indexParty < 0) return;

        playerRef.sendMessage(Lang.msg("party.joined", "leader", totalParty.get(indexParty).leaderUsername)
                .color(Color.orange));
        totalParty.get(indexParty).AddPlayer(playerRef);
    }

    public static void DeclineParty(PlayerRef playerRef) {
        int index = ValidateInvitationToPlayer(playerRef);
        if (index < 0) return;

        int id = GetIndexPartyById(totalInvitations.get(index).id);
        if (id < 0) return;

        totalParty.get(id).players.getFirst().sendMessage(
                Lang.msg("party.invite-declined-leader", "player", playerRef.getUsername()).color(Color.orange));
        String leader = totalParty.get(id).leaderUsername;

        playerRef.sendMessage(Lang.msg("party.invite-declined", "leader", leader));
        totalInvitations.remove(index);
    }

    public static void LeaveParty(PlayerRef playerRef) {
        int index = GetPartyIdForPlayer(playerRef);
        if (index < 0) return;

        PlayerRef leader = totalParty.get(index).players.getFirst();
        totalParty.get(index).RemovePlayer(playerRef);

        if (leader.equals(playerRef)) {
            if (!totalParty.get(index).players.isEmpty()) {
                totalParty.get(index).RemoveHUD(playerRef);
                totalParty.get(index).TransferLeadership();
                totalParty.get(index).leaderUsername = totalParty.get(index).players.getFirst().getUsername();
                totalParty.get(index).UpdateHUD();
            } else {
                int id = totalParty.get(index).id;
                totalInvitations.removeIf(item -> item.id == id);
                totalParty.remove(index);
            }
        } else {
            totalParty.get(index).RemoveHUD(playerRef);
            totalParty.get(index).TransferLeadership();
        }
    }

    public static void OrderParty(PlayerRef playerRef) {
        int id = GetPartyIdForPlayer(playerRef);
        if (id < 0) return;

        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).players.getFirst().sendMessage(
                Lang.msg("party.start-request", "player", playerRef.getUsername()).color(Color.yellow));
    }

    public static void ThrowParty(PlayerRef playerRef) {
        int id = GetPartyIdForPlayer(playerRef);
        if (id < 0) return;

        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).RemovePlayer(playerRef);
        totalParty.get(index).players.getFirst().sendMessage(
                Lang.msg("party.kicked", "player", playerRef.getUsername()).color(Color.yellow));
        totalParty.get(index).TransferLeadership();
    }

    private static int GetInvitationByPlayer(PlayerRef playerRef) {
        int index = -1;

        for (int i = 0; i < totalInvitations.size(); i++) {
            if (totalInvitations.get(i).invited == playerRef) {
                index = i;
                break;
            }
        }

        return index;
    }

    private static String GetLeaderById(int id) {
        String leader = "";

        for (PartyObject item : totalParty) {
            if (item.id == id) {
                leader = item.leaderUsername;
                break;
            }
        }

        if (leader.isEmpty()) return null;

        return leader;
    }

    private static int GetIndexPartyById(int id) {
        int index = -1;
        for (int i = 0; i < totalParty.size(); i++) {
            if (totalParty.get(i).id == id) {
                index = i;
                break;
            }
        }

        return index;
    }

    private static void SendMessageToLeader(int id, Message message) {
        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).players.getFirst().sendMessage(message.color(Color.orange));
    }

    public static int GetPartyIdForPlayer(PlayerRef playerRef) {
        for (int i = 0; i < totalParty.size(); i++) {
            if (totalParty.get(i).players.stream().anyMatch(player -> player == playerRef)) return i;
        }

        return -1;
    }

    private static int ValidateInvitationToPlayer(PlayerRef playerRef) {
        int index = -1;
        for (int i = 0; i < totalInvitations.size(); i++) {
            if (totalInvitations.get(i).invited == playerRef) {
                index = i;
                break;
            }
        }

        return index;
    }

    public static void clearRuntimeState() {
        totalParty.clear();
        totalInvitations.clear();
        partyIdCounter = 0;
    }
}
