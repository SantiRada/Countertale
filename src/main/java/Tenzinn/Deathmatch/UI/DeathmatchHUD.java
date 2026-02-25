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
import java.util.Objects;
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
    }

    public void setShield(PlayerRef playerRef) {
        if (uiBuilder == null) return;
        if(RefactorTool.getSizeSlots() <= 0) return;

        ArrayList<WeaponStats> loot = RefactorTool.getLoot(playerRef);
        if(loot == null) return;
        if(loot.isEmpty()) return;
        if(loot.size() < 2) return;
        if(loot.get(2) == null) return;

        WeaponStats currentShield = loot.get(2);
        if (currentShield == null) return;

        if (currentShield.pos == Objects.requireNonNull(RefactorTool.getSlot(1)).pos) uiBuilder.set("#IconShield.Background", Value.ref("Game/images/weapons/Weapons.ui", "KevlarHelmet"));
        else if (currentShield.pos == Objects.requireNonNull(RefactorTool.getSlot(0)).pos) uiBuilder.set("#IconShield.Background", Value.ref("Game/images/weapons/Weapons.ui", "Kevlar"));
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
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void setWeapons(int value) {
        if (uiBuilder == null || value < 1) return;
        if(RefactorTool.getSizeSlots() <= 0) return;

        ArrayList<WeaponStats> loot = playerStats.getLoot();
        if (loot.isEmpty()) return;

        WeaponStats first = loot.getFirst();
        if (first != null) {
            boolean isPrimary = first.typeWeapon.equalsIgnoreCase("primary");
            String color01 = isPrimary ? (value == 1 ? "#ffffff" : "#ffffff80") : "#ffffff00";
            uiBuilder.set("#Number01.Style.TextColor", color01);
            uiBuilder.set("#Weapon01.Background", (isPrimary ? Value.ref("Game/images/weapons/Weapons.ui", first.image) : "#ffffff00").toString());

            if (isPrimary) {
                uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.get(value - 1).firemode));
                uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.get(value - 1).crossType));
            }
        }

        WeaponStats second = loot.get(1);
        if (second != null && second.typeWeapon.equalsIgnoreCase("secondary")) {
            uiBuilder.set("#Number02.Style.TextColor", value == 2 ? "#ffffff" : "#ffffff80");
            uiBuilder.set("#Weapon02.Background", Value.ref("Game/images/weapons/Weapons.ui", second.image));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", second.firemode));
            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", second.crossType));
        }

        uiBuilder.set("#Number03.Style.TextColor", value >= 3 ? "#ffffff" : "#ffffff80");
        uiBuilder.set("#Weapon03.Background", Value.ref("Game/images/weapons/Weapons.ui", value >= 3 ? "Weapon03" : "Weapon03Off"));
        uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", "Melee"));
        uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", "Knife"));

        update(true, uiBuilder);
    }

    public void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    public void clearHUDVisuals() {
        if (uiBuilder == null) return;

        try {
            uiBuilder.remove("#DeathmatchUI");
            update(true, uiBuilder);
        } catch (Exception e) { }
        uiBuilder = null;
    }

    public void clearHUD() {
        stopTimer();
        clearHUDVisuals();
    }
}