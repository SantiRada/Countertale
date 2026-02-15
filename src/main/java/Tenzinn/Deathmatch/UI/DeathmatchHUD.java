package Tenzinn.Deathmatch.UI;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Objects.WeaponStats;
import Tenzinn.Deathmatch.Objects.PlayerStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class DeathmatchHUD extends CustomUIHud {

    private UICommandBuilder uiBuilder;
    private PlayerRef playerRef;

    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600;

    public WeaponStats[] weapons = new WeaponStats[3];

    public DeathmatchHUD(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
        this.playerRef = playerRef;

        weapons[0] = new WeaponStats("Weapon01", WeaponStats.TypeWeapon.Weapon, WeaponStats.Firemode.Automatic);
        weapons[1] = new WeaponStats("Weapon02", WeaponStats.TypeWeapon.Shotgun, WeaponStats.Firemode.Burst);
        weapons[2] = new WeaponStats("Weapon03", WeaponStats.TypeWeapon.Knife, WeaponStats.Firemode.Melee);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Deathmatch.ui");
        uiBuilder = uiCommandBuilder;

        update(true, uiBuilder);

        setWeapons(1);
        setTimer();
        setData();
    }

    public void setData() {
        List<PlayerStats> playersList = RefactorTool.getPlayerList();
        playersList.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        int maxPlayers = Math.min(playersList.size(), 10);

        for(int index = 1; index <= maxPlayers; index++) {
            PlayerStats player = playersList.get(index - 1);

            uiBuilder.set("#User0" + index + ".TextSpans", Message.raw(String.valueOf(player.getKills())));

            if(player.getPlayerRef().equals(playerRef)) {
                if (index == 1) { uiBuilder.set("#Name0" + index + ".Background", "#27F5A3"); }
                else { uiBuilder.set("#Name0" + index + ".Background", "#E0B448"); }
            }
        }

        for (int i = maxPlayers + 1; i <= 10; i++) {
            uiBuilder.set("#User0" + i + ".TextSpans", Message.raw(""));
            uiBuilder.set("#Name0" + i + ".Background", "#00000040");
        }

        update(true, uiBuilder);
    }

    public void setTimer() {
        timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
                if (stats == null || stats.getCurrentMatch() == null) {
                    if (timerTask != null) timerTask.cancel(false);
                    return;
                }

                remainingSeconds = stats.getCurrentMatch().getTimer();
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timerText = String.format("%02d:%02d", minutes, seconds);

                uiBuilder.set("#TextTimer.TextSpans", Message.raw(timerText));
                update(true, uiBuilder);

            } catch (Exception e) { if (timerTask != null) timerTask.cancel(false); }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void setWeapons(int value) {
        if(weapons.length <= 0) return;

        if (value < 1 || value > 3) return;

        for(int i = 1; i <= weapons.length; i++){
            if(i != value){
                uiBuilder.set("#Number0" + i +".Style.TextColor", "#ffffff80");
                uiBuilder.set("#Weapon0" + i + ".Background", Value.ref("Game/images/weapons/Weapons.ui", weapons[i - 1].nameWeapon + "Off"));
            } else {
                uiBuilder.set("#Number0" + value + ".Style.TextColor", "#ffffff");
                uiBuilder.set("#Weapon0" + value + ".Background", Value.ref("Game/images/weapons/Weapons.ui", weapons[value - 1].nameWeapon));
            }
        }

        uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", weapons[value - 1].firemode));
        uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", weapons[value - 1].typeWeapon));

        if (weapons.length < 3) {
            uiBuilder.set("#Number03.Style.TextColor", "#ffffff00");
            uiBuilder.set("#Weapon03.Background", "#ffffff00");
        }

        update(true, uiBuilder);
    }

    public void clearHUD() {
        if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false);

        uiBuilder.remove("#DeathmatchUI");
        update(true, uiBuilder);
    }
}