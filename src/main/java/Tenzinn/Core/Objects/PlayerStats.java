package Tenzinn.Core.Objects;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Shop.EconomySystem;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.Objects;

public class PlayerStats {

    public int deaths = 0;
    public int kills = 0;
    public int score = 0;

    public float maxHealth = -1;

    public boolean canReceivedLoot = false;
    public int timerCanReceivedLoot = 15;
    public boolean isInvulnerable = false;

    public WeaponStats primaryWeapon = null;
    public WeaponStats secondaryWeapon = null;
    public WeaponStats shield = null;

    public PlayerState playerState;
    public GameMatch currentMatch;

    public WeaponStats utility = null;

    // Economy
    protected int money = 0;
    public boolean inShop = false;
    public ArrayList<Integer> moneySpent = new ArrayList<Integer>();

    public PlayerRef playerRef;
    public Player player;

    public enum PlayerState { DEFAULT, SPECTATOR }
    public enum Effects { INVULNERABILITY, NULL }

    public PlayerStats (PlayerRef playerRef, Player player, GameMatch match) {
        this.playerRef = playerRef;
        this.currentMatch = match;
        this.player = player;
        this.playerState = PlayerState.DEFAULT;

        moneySpent.add(-1);
        moneySpent.add(-1);
        moneySpent.add(-1);
        moneySpent.add(-1);
    }
    // ================================================= //
    public void setKills () {
        kills += 1;
        score += 15;

        money += EconomySystem.getRevenue(EconomySystem.Revenue.KILL);
    }

    public void setDeaths () {
        deaths += 1;
        score = score > 10 ? score - 10 : 0;

        money += EconomySystem.getRevenue(EconomySystem.Revenue.DEATH);
    }

    public void setScore(int value) {
        score += value;
    }
    public void setHealth(int value) { maxHealth = value; }
    public void setMoney(int value) { money -= value; }
    public void giveMoney(int value) { money += value; }
    public void setFinishBuyZone() {
        inShop = false;
        moneySpent.set(0, -1);
        moneySpent.set(1, -1);
        moneySpent.set(2, -1);
        moneySpent.set(3, -1);
    }
    // ================================================= //
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getScore() { return score; }
    public int getMoney() { return money; }
    public ArrayList<WeaponStats> getLoot() {
        ArrayList<WeaponStats> list = new ArrayList<>();

        if(primaryWeapon != null) list.add(primaryWeapon);
        if(secondaryWeapon != null) list.add(secondaryWeapon);
        if(shield != null) list.add(shield);

        if (utility != null) list.add(utility);

        return list;
    }
    // ================================================= //
    public GameMatch getCurrentMatch () { return currentMatch; }
    public PlayerRef getPlayerRef() { return playerRef; }
    public Player getPlayer() { return player; }
}