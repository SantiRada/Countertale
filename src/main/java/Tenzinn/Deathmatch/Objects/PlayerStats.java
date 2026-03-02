package Tenzinn.Deathmatch.Objects;

import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;

public class PlayerStats {

    public int deaths = 0;
    public int kills = 0;
    public int score = 0;

    public int damageCaused = 0;
    public int damageReceived = 0;
    public int meleeDamage = 0;

    public float maxHealth = -1;

    public boolean canReceivedLoot = false;
    public int timerCanReceivedLoot = 15;
    public boolean isInvulnerable = false;

    public WeaponStats primaryWeapon = null;
    public WeaponStats secondaryWeapon = null;
    public WeaponStats shield = null;

    public GameMatch currentMatch;

    public PlayerRef playerRef;
    public Player player;

    public enum Effects { INVULNERABILITY, NULL }

    public PlayerStats (PlayerRef playerRef, Player player, GameMatch match) { this.playerRef = playerRef; this.player = player; this.currentMatch = match; }
    // ================================================= //
    public void setKills () {
        kills += 1;
        score += 15;

        RefactorTool.setChangesInUI();
    }
    public void setDeaths () {
        deaths += 1;
        score = score > 10 ? score - 10 : 0;

        RefactorTool.setChangesInUI();
    }
    public void setScore(int value) {
        score += value;

        RefactorTool.setChangesInUI();
    }
    public void setHealth(int value) { maxHealth = value; }
    // ================================================= //
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getScore() { return score; }
    public ArrayList<WeaponStats> getLoot() {
        ArrayList<WeaponStats> list = new ArrayList<>();

        if(primaryWeapon != null) list.add(primaryWeapon);
        if(secondaryWeapon != null) list.add(secondaryWeapon);
        if(shield != null) list.add(shield);

        return list;
    }
    // ================================================= //
    public GameMatch getCurrentMatch () { return currentMatch; }
    public PlayerRef getPlayerRef() { return playerRef; }
    public Player getPlayer() { return player; }
    public String getName() { return player.getDisplayName(); }
}