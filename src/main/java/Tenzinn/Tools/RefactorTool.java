package Tenzinn.Tools;

import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.LootManager;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.UI.MvpPage;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.Deathmatch.Objects.PlayerStats;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import Tenzinn.Listeners.MessageListeners;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class RefactorTool {

    public enum TypeData { SCORE, DEATH, KILL }
    public static List<PlayerStats> playerStatsList = new ArrayList<>();
    public static ArrayList<WeaponStats> slots = new ArrayList<>();

    private static Vector3d[] spawns = {
            new Vector3d(-27, 107, -10),
            new Vector3d(-28, 107, 10),
            new Vector3d(-10, 110, -13),
            new Vector3d(-31, 112, -23),
            new Vector3d(-20, 110, -43),
            new Vector3d(18, 106, -36),
            new Vector3d(14, 110, -15),
            new Vector3d(40, 110, -13),
            new Vector3d(0, 111, 32),
            new Vector3d(-33, 110, 28)
    };

    public static void setPlayerStats (PlayerStats playerStats) { playerStatsList.add(playerStats); }
    public static void setQuitPlayerStats (PlayerStats playerStats) { playerStatsList.remove(playerStats); }
    public static void setSlots (ArrayList<WeaponStats> newSlots){
        slots.clear();
        slots.addAll(newSlots);
    }
    public static void setLoot(PlayerRef playerRef, int index) {
        if (getSizeSlots() <= 0) return;

        WeaponStats newWeapon = slots.get(index - 1);

        if (newWeapon.nameWeapon.equalsIgnoreCase("comingsoon")) return;

        String message = MessageListeners.get(MessageListeners.MessageKey.CHAT_WHEN_BUYING);
        playerRef.sendMessage(Message.raw(message + newWeapon.nameWeapon).color(Color.cyan));

        for (PlayerStats stats : playerStatsList) {
            if(stats.getPlayerRef() == playerRef) {
                switch (newWeapon.typeWeapon.toLowerCase()) {
                    case "primary":     stats.primaryWeapon = newWeapon;    break;
                    case "secondary":   stats.secondaryWeapon = newWeapon;  break;
                    case "shield":      stats.shield = newWeapon;           break;
                }

                if (!stats.canReceivedLoot && !stats.getCurrentMatch().isBuyPhase()) {
                    playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_BUYING_LATE)).color(Color.yellow));
                }
                else { LootManager.giveLoot(stats.getPlayer(), getLoot(stats.getPlayerRef())); }

                break;
            }
        }
    }
    public static void setAllLoot(PlayerRef playerRef, ArrayList<WeaponStats> list) {
        for(PlayerStats stats : playerStatsList) {
            if(stats.getPlayerRef().equals(playerRef)) {
                if (list.size() > 2) {
                    if(list.get(0).typeWeapon.equalsIgnoreCase("primary")) stats.primaryWeapon = list.get(0);
                    if(list.get(1).typeWeapon.equalsIgnoreCase("secondary")) stats.secondaryWeapon = list.get(1);
                    if(list.get(2).typeWeapon.equalsIgnoreCase("shield")) stats.shield = list.get(2);
                } else {
                    stats.primaryWeapon = null;
                    if(list.get(0).typeWeapon.equalsIgnoreCase("secondary")) stats.secondaryWeapon = list.get(0);
                    if(list.get(1).typeWeapon.equalsIgnoreCase("shield")) stats.shield = list.get(1);
                }
            }
        }
    }
    // ============================================ //
    public static ArrayList<WeaponStats> getLoot(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if(playerStats == null) return null;

        ArrayList<WeaponStats> list = new ArrayList<>();

        if(playerStats.primaryWeapon != null) list.add(playerStats.primaryWeapon);

        if(playerStats.secondaryWeapon != null) list.add(playerStats.secondaryWeapon);

        if(playerStats.shield != null) list.add(playerStats.shield);

        return !list.isEmpty() ? list : null;
    }
    public static WeaponStats getSlot(int id) {
        if(slots.size() <= id) return null;
        return slots.get(id);
    }
    public static int getSizeSlots () { return slots.size(); }
    public static PlayerStats getPlayerStats(PlayerRef playerRef) {
        if (playerStatsList.isEmpty()) return null;

        for (PlayerStats playerStats : playerStatsList) {
            if (playerStats.getPlayerRef().equals(playerRef)) { return playerStats; }
        }
        return null;
    }
    public static List<PlayerStats> getPlayerList() { return playerStatsList; }
    // ============================================ //
    public static Player getPlayer(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        Player player = store.getComponent(ref, Player.getComponentType());

        return player;
    }
    // ============================================ //
    public static void Respawn(PlayerRef playerRef) {
        World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());

        if (currentWorld == null) { return; }

        Random random = new Random();
        int randomPosition = random.nextInt(10);
        Vector3d spawnPos = spawns[randomPosition];

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            currentWorld.execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    Store<EntityStore> store = ref.getStore();

                    Vector3f rotation = new Vector3f(0f, 0f, 0f);
                    Vector3f targetPosition = new Vector3f((float) spawnPos.getX(), (float) spawnPos.getY(), (float) spawnPos.getZ());

                    Teleport teleport = new Teleport(targetPosition.toVector3d(), rotation);
                    store.putComponent(ref, Teleport.getComponentType(), teleport);
                }  catch (Exception e) { e.printStackTrace(); }
            });
        }, 150, TimeUnit.MILLISECONDS);

        PlayerStats playerStats = getPlayerStats(playerRef);
        if(playerStats == null) return;

        CustomUIHud customHUD = RefactorTool.getPlayer(playerRef).getHudManager().getCustomHud();
        boolean isInGame = false;
        if(customHUD instanceof DeathmatchHUD) { isInGame = true; }
        DeathmatchHUD newHud = isInGame ? (DeathmatchHUD) customHUD : null;
        if (newHud == null) return;

        playerStats.isInvulnerable = true;
        newHud.setEffect(PlayerStats.Effects.INVULNERABILITY);

        if(!playerStats.getCurrentMatch().isBuyPhase()) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                playerStats.isInvulnerable = false;
                newHud.setEffect(PlayerStats.Effects.NULL);
            }, 3, TimeUnit.SECONDS);
        }

        playerStats.canReceivedLoot = true;
        playerStats.timerCanReceivedLoot = 15;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            playerStats.canReceivedLoot = false;

            newHud.setEffect(PlayerStats.Effects.NULL);
            playerStats.isInvulnerable = false;
            }, 15, TimeUnit.SECONDS);

        setShield(playerRef);
    }
    // ============================================ //
    public static void setShield(PlayerRef playerRef) {
        CustomUIHud customHUD = RefactorTool.getPlayer(playerRef).getHudManager().getCustomHud();

        if(customHUD instanceof DeathmatchHUD newHUD) { newHUD.setShield(playerRef); }
    }
    // ============================================ //
    public static void setChangesInUI() {
        for (PlayerStats playerStats : playerStatsList) {
            if(playerStats.getPlayer().getHudManager().getCustomHud() == null) continue;

            Object testHud = playerStats.getPlayer().getHudManager().getCustomHud();
            if(testHud instanceof DeathmatchHUD) {
                DeathmatchHUD deathmatchHUD = (DeathmatchHUD)testHud;
                deathmatchHUD.setData();
            } else if(testHud instanceof ScoreboardPage) {
                ScoreboardPage scoreboard = (ScoreboardPage)testHud;
                scoreboard.setData();
            }
        }
    }
    public static void setChangesInSlots (int value, PlayerRef playerRef) {
        Player player = getPlayer(playerRef);
        if(player == null) return;

        Object testHud = player.getHudManager().getCustomHud();

        if(testHud == null) return;

        if(testHud instanceof DeathmatchHUD) {
            DeathmatchHUD currentHUD = (DeathmatchHUD) testHud;

            if(value > 3) return;

            currentHUD.setWeapons(value);
        }
    }
    public static void setDataScore(Player player, TypeData typeData, float value) {
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        PlayerStats playerStats = getPlayerStats(playerRef);
        assert playerStats != null;

        switch (typeData) {
            case TypeData.SCORE: int finalValue = (int)value; playerStats.setScore(finalValue); break;
            case TypeData.DEATH: playerStats.setDeaths(); break;
            case TypeData.KILL: playerStats.setKills(); break;
        }
    }
    // ============================================ //
    public static void finishGame(List<PlayerRef> players) {
        for (PlayerRef playerRef : players) {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();

            playerRef.sendMessage(Message.raw("Se terminó la partida").color(Color.cyan));

            Player player = getPlayer(playerRef);
            player.getPageManager().openCustomPage(ref, store, new MvpPage(playerRef));
        }
    }
    public static void setMeleeDamage (Player player, float value) {
        PlayerStats playerStats = getPlayerStats(Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT));

        playerStats.meleeDamage += value;
    }
    public static void setDamageReceived (Player player, float value) {
        PlayerStats playerStats = getPlayerStats(Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT));

        playerStats.damageReceived += value;
    }
    public static void setDamageCaused (Player player, float value) {
        PlayerStats playerStats = getPlayerStats(Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT));

        playerStats.damageCaused += value;
    }
}