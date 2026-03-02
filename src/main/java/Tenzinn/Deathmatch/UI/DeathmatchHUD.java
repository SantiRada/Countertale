package Tenzinn.Deathmatch.UI;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Events.PlayerHealthTracker;
import Tenzinn.Deathmatch.Objects.WeaponStats;
import Tenzinn.Deathmatch.Objects.PlayerStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class DeathmatchHUD extends CustomUIHud {

    private UICommandBuilder uiBuilder;

    private final PlayerRef playerRef;
    private final PlayerStats playerStats;

    public ScheduledFuture<?> timerTask;
    private int remainingSeconds = 600;

    public DeathmatchHUD(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
        this.playerRef = playerRef;

        playerStats = RefactorTool.getPlayerStats(playerRef);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Game/Deathmatch.ui");
        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();

        updateHealth();

        setShield();
        setWeapons(2);
    }

    public void setEffect(PlayerStats.Effects effect) {
        switch (effect) {
            case PlayerStats.Effects.INVULNERABILITY: uiBuilder.set("#DeathmatchUI.Background", Value.ref("Game/Deathmatch.ui", "Invulnerability")); break;
            case PlayerStats.Effects.NULL: uiBuilder.set("#DeathmatchUI.Background", "#ffffff00"); break;
        }

        update(true, uiBuilder);
    }

    public void setShield() {
        if (uiBuilder == null) return;

        ArrayList<WeaponStats> loot = new ArrayList<>(playerStats.getLoot());
        if (loot.isEmpty()) return;

        WeaponStats shield = loot.stream().filter(ps -> ps.typeWeapon.equalsIgnoreCase("shield")).findFirst().orElse(null);
        if (shield == null) {
            playerRef.sendMessage(Message.raw("No se encuentra escudo en este jugador."));
            return;
        }

        uiBuilder.set("#IconShield.Background", Value.ref("Game/images/weapons/Weapons.ui", shield.image));
        update(true, uiBuilder);
    }

    public void setWeapons(int value) {
        if (uiBuilder == null) return;
        if (RefactorTool.getSizeSlots() <= 0) {
            playerRef.sendMessage(Message.raw("No cargaron los slots.").color(Color.red));
            return;
        }

        ArrayList<WeaponStats> loot = playerStats.getLoot();
        if (loot.isEmpty()) {
            playerRef.sendMessage(Message.raw("No cargó el loot.").color(Color.red));
            return;
        }

        if (!loot.get(0).typeWeapon.equalsIgnoreCase("primary")) {
            uiBuilder.set("#Number01.Style.TextColor", "#ffffff00");
            uiBuilder.set("#Weapon01.Background", "#ffffff00");

            String colorText = value == 2 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number02.Style.TextColor", colorText);

            uiBuilder.set("#Weapon02.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().image));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.getFirst().crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().firemode));
        }
        else {
            String colorText = value == 1 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number01.Style.TextColor", colorText);

            uiBuilder.set("#Weapon01.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().image));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.getFirst().crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().firemode));
        }

        if(loot.get(1).typeWeapon.equalsIgnoreCase("secondary")) {
            String colorText = value == 2 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number02.Style.TextColor", colorText);

            uiBuilder.set("#Weapon02.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.get(1).image));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.get(1).crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.get(1).firemode));
        }

        // Knife
        uiBuilder.set("#Number03.Style.TextColor", value >= 3 ? "#ffffff" : "#ffffff80");
        uiBuilder.set("#Weapon03.Background", Value.ref("Game/images/weapons/Weapons.ui", value >= 3 ? "Weapon03" : "Weapon03Off"));

        if (value >= 3) {
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", "Melee"));
            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", "Knife"));
        }

        update(true, uiBuilder);
    }

    public void setHealth(int value, int max) {
        if (uiBuilder == null) return;

        int newHealth = value * 61 / max;

        int lifeValue = Math.min(value, 100);

        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(newHealth));
        anchor.setHeight(Value.of(7));

        if (value > 100) { uiBuilder.set("#NumberShield.TextSpans", Message.raw(String.valueOf(value - 100))); }
        else {
            uiBuilder.set("#NumberShield.TextSpans", Message.raw(""));
            uiBuilder.set("#IconShield.Background", "#ffffff00");
        }

        uiBuilder.set("#HealthNumber.TextSpans", Message.raw(String.valueOf(lifeValue)));
        uiBuilder.setObject("#Slider.Anchor", anchor);

        update(true, uiBuilder);
    }

    public void updateHealth() {
        UUID uuid = playerRef.getUuid();

        float current = PlayerHealthTracker.getCurrentHealth(uuid);
        float max = PlayerHealthTracker.getCurrentHealth(uuid);

        setHealth((int)current, (int)max);
    }

    public void setData() {
        if (uiBuilder == null) return;

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
        if (uiBuilder == null) return;

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
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    public void clearHUD() {
        if (uiBuilder == null) return;

        stopTimer();

        try {
            uiBuilder.remove("#DeathmatchUI");
            update(true, uiBuilder);
        } catch (Exception e) { }
        uiBuilder = null;
    }
}