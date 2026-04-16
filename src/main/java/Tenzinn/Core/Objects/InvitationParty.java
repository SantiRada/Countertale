package Tenzinn.Core.Objects;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class InvitationParty {
    public int id;
    public PlayerRef invited;
    
    public InvitationParty (int id, PlayerRef invited) {
        this.id = id;
        this.invited = invited;
    }
}