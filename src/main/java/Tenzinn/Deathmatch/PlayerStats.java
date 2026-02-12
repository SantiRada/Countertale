package Tenzinn.Deathmatch;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerStats {

    public int kills = 0;
    public int deaths = 0;

    public GameMatch currentMatch;

    public PlayerRef playerRef;
    public Player player;

    public PlayerStats (PlayerRef playerRef, Player player, GameMatch match) { this.playerRef = playerRef; this.player = player; this.currentMatch = match; }
    public void resetStats() { kills = deaths = 0; currentMatch = null; }
    // ================================================= //
    public void setKills () { kills += 1; }
    public void setDeaths () { deaths += 1; }
    // ================================================= //
    public int getKills() { return kills; }
    public String getKD() {

        String killsText = String.valueOf(kills);
        String deathText = String.valueOf(deaths);

        if(String.valueOf(kills).length() < 2) { killsText = "0" + kills; }
        if(String.valueOf(deaths).length() < 2) { deathText = "0" + deathText; }

        return killsText + " / " + deathText;
    }
    // ================================================= //
    public PlayerRef getPlayerRef() { return playerRef; }
    public Player getPlayer() { return player; }
}