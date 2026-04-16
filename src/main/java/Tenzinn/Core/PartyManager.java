package Tenzinn.Core;

import Tenzinn.Core.Objects.PartyObject;
import Tenzinn.Core.Objects.InvitationParty;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class PartyManager {

    public static List<PartyObject> totalParty = new ArrayList<>();
    public static List<InvitationParty> totalInvitations = new ArrayList<>();

    public static void CreateParty(PlayerRef playerRef) {
        for (PartyObject partyObject : totalParty) {
            if (partyObject.players.stream().anyMatch(player -> player == playerRef)) {
                playerRef.sendMessage(Message.raw("No puedes crear otro grupo si formas parte de uno. Puedes abandonarlo con /party leave"));
                return;
            }
        }

        PartyObject newParty = new PartyObject(totalParty.size(), playerRef);
        totalParty.add(newParty);
        newParty.TransferLeadership();
        playerRef.sendMessage(Message.raw("Created party!").color(Color.cyan));
    }
    public static void InviteToParty(int id, PlayerRef playerRef) {
        String leader = GetLeaderById(id);
        if(leader == null) return;

        // Revisar si el usuario ya tiene una invitación
        int testInvitation = GetInvitationByPlayer(playerRef);
        if(testInvitation >= 0) {
            playerRef.sendMessage(Message.raw("Tienes una invitación pendiente. Otros grupos no pueden invitarte hasta que la respondas.").color(Color.orange));
            return;
        }

        SendMessageToLeader(id, "Se envió la invitación al jugador " + playerRef.getUsername());

        // Enviar el mensaje de la invitación
        playerRef.sendMessage(Message.raw("Has sido invitado a unirte al grupo de " + leader + " para aceptar escribe /party join").color(Color.cyan));

        // Crear invitación en memoria
        InvitationParty newIntivation = new InvitationParty(id, playerRef);
        totalInvitations.add(newIntivation);
    }
    public static void JoinToParty(PlayerRef playerRef) {
        int index = ValidateInvitationToPlayer(playerRef);
        if (index < 0) return;

        int indexParty = GetIndexPartyById(totalInvitations.get(index).id);
        if (indexParty < 0) return;

        playerRef.sendMessage(Message.raw("Has entrado al grupo de " + totalParty.get(indexParty).leaderUsername).color(Color.orange));
        totalParty.get(indexParty).AddPlayer(playerRef);
    }
    public static void DeclineParty(PlayerRef playerRef) {
        int index = ValidateInvitationToPlayer(playerRef);
        if (index < 0) return;

        int id = GetIndexPartyById(totalInvitations.get(index).id);
        if (id < 0) return;

        totalParty.get(id).players.getFirst().sendMessage(Message.raw("El jugador" + playerRef.getUsername() + " rechazó tu invitación.").color(Color.orange));
        String leader = totalParty.get(id).leaderUsername;

        playerRef.sendMessage(Message.raw("Rechazaste la invitación al grupo de " + leader));
        totalInvitations.remove(index);
    }
    public static void LeaveParty(PlayerRef playerRef) {
        int index = GetPartyIdForPlayer(playerRef);
        if (index < 0) return;

        PlayerRef leader = totalParty.get(index).players.getFirst();

        totalParty.get(index).RemovePlayer(playerRef);
        totalParty.get(index).RemoveHUD(playerRef);

        if (leader.equals(playerRef)) {
            // Sos el leader
            if (!totalParty.get(index).players.isEmpty()) {
                // Se transfiere el liderazgo
                totalParty.get(index).TransferLeadership();
                totalParty.get(index).leaderUsername = totalParty.get(index).players.getFirst().getUsername();
                totalParty.get(index).UpdateHUD();
            } else {
                int id = totalParty.get(index).id;
                totalInvitations.removeIf(item -> item.id == id);
                totalParty.remove(index);
            }
        }
    }
    public static void OrderParty(PlayerRef playerRef) {
        int id = GetPartyIdForPlayer(playerRef);
        if (id < 0) return;

        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).players.getFirst().sendMessage(Message.raw(playerRef.getUsername() + ", ¡pide que inicies la partida!").color(Color.yellow));
    }
    public static void ThrowParty(PlayerRef playerRef) {
        int id = GetPartyIdForPlayer(playerRef);
        if (id < 0) return;

        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).RemovePlayer(playerRef);

        totalParty.get(index).players.getFirst().sendMessage(Message.raw("Has eliminado al jugador " + playerRef.getUsername() + " del grupo.").color(Color.yellow));
    }
    // ============================================= //
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

        if(leader.isEmpty()) return null;

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
    private static void SendMessageToLeader(int id, String message) {
        int index = GetIndexPartyById(id);
        if (index < 0) return;

        totalParty.get(index).players.getFirst().sendMessage(Message.raw(message).color(Color.orange));
    }
    public static int GetPartyIdForPlayer(PlayerRef playerRef) {
        for (int i = 0; i < totalParty.size(); i++) {
            if (totalParty.get(i).players.stream().anyMatch(player -> player == playerRef)) { return i; }
        }

        return -1;
    }
    private static int ValidateInvitationToPlayer(PlayerRef playerRef) {
        int index = -1;
        for (int i = 0; i < totalInvitations.size(); i++) { if(totalInvitations.get(i).invited == playerRef) { index = i; break; } }

        return index;
    }
}