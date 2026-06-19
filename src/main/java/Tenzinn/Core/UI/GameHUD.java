package Tenzinn.Core.UI;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Events.PlayerHealthTracker;
import Tenzinn.Core.Localization.Lang;

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
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameHUD extends CustomUIHud {

    private static final Set<GameHUD> ACTIVE_HUDS = ConcurrentHashMap.newKeySet();
    private static ScheduledFuture<?> sharedTimerTask;

    private UICommandBuilder uiBuilder;

    private final PlayerRef playerRef;
    private final PlayerStats playerStats;
    private String mode;

    private final HudTimer matchTimer = new HudTimer();
    private final HudTimer shopTimer = new HudTimer();
    private final HudTimer invulnerabilityTimer = new HudTimer();

    public GameHUD(@NonNullDecl PlayerRef playerRef) {
        super(playerRef, playerRef.getUuid().toString());
        this.playerRef = playerRef;

        playerStats = RefactorTool.getPlayerStats(playerRef);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        mode = playerStats.getCurrentMatch().getMode();

        if (mode.equalsIgnoreCase("fvf")) { uiCommandBuilder.append("Game/FVF/HUDFVF.ui"); }
        else { uiCommandBuilder.append("Game/HUD.ui"); }

        uiBuilder = uiCommandBuilder;

        setTimer();
        setData();

        updateHealth();

        setShield();
        setWeapons(2);

        uiBuilder.set("#DeathmatchUI.Background", "#ffffff00");
        update(true, uiBuilder);
    }
    // ================================================== //
    public void setEffect(PlayerStats.Effects effect) {
        if (mode.equalsIgnoreCase("fvf")) return;

        switch (effect) {
            case PlayerStats.Effects.INVULNERABILITY: uiBuilder.set("#DeathmatchUI.Background", Value.ref("Game/HUD.ui", "Invulnerability")); break;
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
            playerRef.sendMessage(Lang.msg("hud.shield-not-found"));
            return;
        }

        uiBuilder.set("#IconShield.Background", Value.ref("Game/images/weapons/Weapons.ui", shield.image));
        update(true, uiBuilder);
    }
    public void setWeapons(int value) {
        if (uiBuilder == null) return;
        if (RefactorTool.getSizeSlots() <= 0) {
            playerRef.sendMessage(Lang.msg("hud.slots-not-loaded").color(Color.red));
            return;
        }

        ArrayList<WeaponStats> loot = playerStats.getLoot();
        if (loot.isEmpty()) {
            playerRef.sendMessage(Lang.msg("hud.loot-not-loaded").color(Color.red));
            return;
        }

        if (!loot.get(0).typeWeapon.equalsIgnoreCase("primary")) {
            uiBuilder.set("#Number01.Style.TextColor", "#ffffff00");
            uiBuilder.set("#Weapon01.Background", "#ffffff00");

            String colorText = value == 2 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number02.Style.TextColor", colorText);

            uiBuilder.set("#Weapon02.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().image + "off"));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.getFirst().crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().firemode));
        }
        else {
            String colorText = value == 1 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number01.Style.TextColor", colorText);

            uiBuilder.set("#Weapon01.Background", Value.ref("Game/images/weapons/Weapons.ui", value == 1 ? loot.getFirst().image + "on" : loot.getFirst().image + "off"));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.getFirst().crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.getFirst().firemode));
        }

        if(loot.get(1).typeWeapon.equalsIgnoreCase("secondary")) {
            String colorText = value == 2 ? "#ffffff" : "#ffffff80";
            uiBuilder.set("#Number02.Style.TextColor", colorText);

            uiBuilder.set("#Weapon02.Background", Value.ref("Game/images/weapons/Weapons.ui", value == 2 ? loot.get(1).image + "on" : loot.get(1).image + "off"));

            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", loot.get(1).crossType));
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", loot.get(1).firemode));
        }

        // Knife
        uiBuilder.set("#Number03.Style.TextColor", value >= 3 ? "#ffffff" : "#ffffff80");
        uiBuilder.set("#Weapon03.Background", Value.ref("Game/images/weapons/Weapons.ui", value >= 3 ? "Knifeon" : "Knifeoff"));

        if (value >= 3) {
            uiBuilder.set("#IconBullet.Background", Value.ref("Game/images/weapons/Weapons.ui", "Melee"));
            uiBuilder.set("#Crosshair.Background", Value.ref("Game/images/weapons/Crosshair.ui", "Knife"));
        }

        update(true, uiBuilder);
    }
    public void setHealth(int value, int max) {
        if (uiBuilder == null) return;
        if (max <= 0) return;

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
        float max = PlayerHealthTracker.getMaxHealth(uuid);

        setHealth((int)current, (int)max);
    }
    public void setShopTimer() {
        uiBuilder.set("#ShopSectorTimer.Visible", true);
        shopTimer.start(15);
        registerTicker(this);
    }
    public void setInvulnerability() {
        if (mode.equalsIgnoreCase("fvf")) return;

        uiBuilder.set("#InvulnerabilitySector.Visible", true);
        invulnerabilityTimer.start(3);
        registerTicker(this);
    }
    public void setData() {
        if (uiBuilder == null) return;
        if (mode.equalsIgnoreCase("fvf")) return;

        uiBuilder.set("#InvulnerabilitySector.Visible", false);
        uiBuilder.set("#ShopSectorTimer.Visible", false);

        List<PlayerStats> playersList = RefactorTool.getPlayerList(Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch());
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
    // ================================================== //
    public void setRounds(int team1, int team2) {
        uiBuilder.set("#TextRound.TextSpans", Message.raw(team1 + " | " + team2));
        updateHealth();

        update(true, uiBuilder);
    }
    // ================================================== //
    public void setTimer() {
        if (uiBuilder == null) return;
        stopTimer();
        matchTimer.start();
        registerTicker(this);
    }
    public void stopTimer() {
        matchTimer.stop();
        shopTimer.stop();
        invulnerabilityTimer.stop();
        unregisterTicker(this);
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
    // ================================================== //
    private static synchronized void registerTicker(GameHUD hud) {
        ACTIVE_HUDS.add(hud);
        if (sharedTimerTask == null || sharedTimerTask.isDone()) {
            sharedTimerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(GameHUD::tickAll, 0, 1, TimeUnit.SECONDS);
        }
    }

    private static synchronized void unregisterTicker(GameHUD hud) {
        ACTIVE_HUDS.remove(hud);
        if (ACTIVE_HUDS.isEmpty() && sharedTimerTask != null && !sharedTimerTask.isDone()) {
            sharedTimerTask.cancel(false);
            sharedTimerTask = null;
        }
    }

    private static void tickAll() {
        for (GameHUD hud : ACTIVE_HUDS) {
            try {
                hud.tickTimers();
            } catch (Exception e) {
                unregisterTicker(hud);
            }
        }
    }

    private void tickTimers() {
        if (uiBuilder == null) {
            stopTimer();
            return;
        }

        boolean hasWork = false;

        if (matchTimer.isActive()) {
            hasWork = true;
            PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
            if (stats == null || stats.getCurrentMatch() == null) {
                stopTimer();
                return;
            }

            int remainingSeconds = stats.getCurrentMatch().getTimer();
            if (remainingSeconds > 0) {
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timerText = String.format("%02d:%02d", minutes, seconds);
                if (matchTimer.shouldRender(timerText)) {
                    uiBuilder.set("#TextTimer.TextSpans", Message.raw(timerText));
                    update(true, uiBuilder);
                }
            }
        }

        if (shopTimer.isActive()) {
            hasWork = true;
            if (shopTimer.hasTimeLeft()) {
                String timerText = shopTimer.formatTwoDigits() + "s";
                if (shopTimer.shouldRender(timerText)) {
                    uiBuilder.set("#ShopTimer.TextSpans", Message.raw(timerText));
                    update(true, uiBuilder);
                }
                shopTimer.decrement();
            } else {
                shopTimer.stop();
                uiBuilder.set("#ShopSectorTimer.Visible", false);
                update(true, uiBuilder);
            }
        }

        if (invulnerabilityTimer.isActive()) {
            hasWork = true;
            if (invulnerabilityTimer.hasTimeLeft()) {
                String timerText = invulnerabilityTimer.formatTwoDigits() + "s";
                if (invulnerabilityTimer.shouldRender(timerText)) {
                    uiBuilder.set("#InvulnerabilityTimer.TextSpans", Message.raw(timerText));
                    update(true, uiBuilder);
                }
                invulnerabilityTimer.decrement();
            } else {
                invulnerabilityTimer.stop();
                uiBuilder.set("#InvulnerabilitySector.Visible", false);
                update(true, uiBuilder);
            }
        }

        if (!hasWork) unregisterTicker(this);
    }

    public static synchronized void clearRuntimeState() {
        for (GameHUD hud : new ArrayList<>(ACTIVE_HUDS)) {
            hud.matchTimer.stop();
            hud.shopTimer.stop();
            hud.invulnerabilityTimer.stop();
        }
        ACTIVE_HUDS.clear();
        if (sharedTimerTask != null && !sharedTimerTask.isDone()) {
            sharedTimerTask.cancel(false);
        }
        sharedTimerTask = null;
    }

    private static final class HudTimer {
        private int remainingSeconds;
        private boolean active;
        private String lastText = "";

        void start() {
            active = true;
            lastText = "";
        }

        void start(int remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
            start();
        }

        void stop() {
            active = false;
            lastText = "";
        }

        boolean isActive() { return active; }

        boolean hasTimeLeft() { return remainingSeconds > 0; }

        void decrement() { remainingSeconds -= 1; }

        String formatTwoDigits() { return remainingSeconds > 9 ? String.valueOf(remainingSeconds) : "0" + remainingSeconds; }

        boolean shouldRender(String text) {
            if (text.equals(lastText)) return false;
            lastText = text;
            return true;
        }
    }
}
