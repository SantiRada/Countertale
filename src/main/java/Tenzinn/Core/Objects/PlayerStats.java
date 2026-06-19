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
    private float damageCaused = 0;
    private float damageReceived = 0;
    private float meleeDamage = 0;

    public float maxHealth = -1;

    public boolean canReceivedLoot = false;
    public int timerCanReceivedLoot = 15;
    public boolean isInvulnerable = false;

    public WeaponStats primaryWeapon = null;
    public WeaponStats secondaryWeapon = null;
    public WeaponStats shield = null;

    public PlayerState playerState;
    public GameMatch currentMatch;

    // Economy
    protected int money = 0;
    public boolean inShop = false;
    public ArrayList<Integer> moneySpent = new ArrayList<Integer>();

    // Set to true when a case drops at end of match; cleared by MvpPage/EndMatchPage
    public boolean hasPendingCase = false;

    public PlayerRef playerRef;
    public Player player;

    public enum PlayerState { DEFAULT, SPECTATOR }
    public enum Effects { INVULNERABILITY, NULL }

    public PlayerStats (PlayerRef playerRef, Player player, GameMatch match) {
        this.playerRef = playerRef;
        this.currentMatch = match;
        this.player = player;

        moneySpent.add(-1);
        moneySpent.add(-1);
        moneySpent.add(-1);
    }
    // ================================================= //
    public void setKills () {
        kills += 1;
        score += 15;

        money += EconomySystem.getRevenue(EconomySystem.Revenue.KILL);

        RefactorTool.setChangesInUI(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
    }
    public void setDeaths () {
        deaths += 1;
        score = score > 10 ? score - 10 : 0;

        money += EconomySystem.getRevenue(EconomySystem.Revenue.DEATH);

        RefactorTool.setChangesInUI(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
    }
    public void setScore(int value) {
        score += value;

        RefactorTool.setChangesInUI(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
    }
    public void addDamageCaused(float value, boolean melee) {
        if (value <= 0) return;
        damageCaused += value;
        if (melee) meleeDamage += value;
    }
    public void addDamageReceived(float value) {
        if (value <= 0) return;
        damageReceived += value;
    }
    public void setHealth(int value) { maxHealth = value; }
    public void setMoney(int value) { money -= value; }
    public void giveMoney(int value) { money += value; }
    public void setFinishBuyZone() {
        inShop = false;
        moneySpent.set(0, -1);
        moneySpent.set(1, -1);
        moneySpent.set(2, -1);
    }
    // ================================================= //
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getScore() { return score; }
    public int getDamageCaused() { return Math.round(damageCaused); }
    public int getDamageReceived() { return Math.round(damageReceived); }
    public int getMeleeDamage() { return Math.round(meleeDamage); }
    public int getMoney() { return money; }
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
}
