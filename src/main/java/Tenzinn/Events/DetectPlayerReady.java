package Tenzinn.Events;

import Tenzinn.Deathmatch.UI.DeathmatchHUD;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.awt.*;
import java.util.Random;

public class DetectPlayerReady {

    private static DeathmatchHUD deathmatchHUD = null;
    private static Transform[] spawns = {
            new Transform(36, 56, 0),
            new Transform(14, 52, -9),
            new Transform(5, 52, -4),
            new Transform(4, 52, 2),
            new Transform(8, 52, 9),
            new Transform(16, 52, 10),
            new Transform(8, 52, 4),
            new Transform(1, 56, 1),
            new Transform(21, 56, -11),
            new Transform(-2, 59, 2)
    };

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        if(player.getWorld().getName().equals("default")) {
            if(deathmatchHUD != null) { deathmatchHUD.clearHUD(); }

            getLobbyLoot(player);
        }
        else {
            openGameHud(playerRef, player);
            getGameLoot(player);

            World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
            Ref<EntityStore> ref = playerRef.getReference();

            if (currentWorld == null) {
                player.sendMessage(Message.raw("No se encontró el mundo"));
                return;
            }

            currentWorld.execute(() -> {
                try {
                    player.sendMessage(Message.raw("Inicia el TP"));
                    Store<EntityStore> store = ref.getStore();

                    Random random = new Random();
                    int randomPosition = random.nextInt(10);

                    Teleport teleport = Teleport.createForPlayer(currentWorld, spawns[randomPosition]);
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                    player.sendMessage(Message.raw("Debería haber TP"));
                } catch (Exception e) { e.printStackTrace();
                    player.sendMessage(Message.raw("Falló el TP")); }
            });
        }
    }
    public static void openGameHud(PlayerRef playerRef, Player player) {
        DeathmatchHUD newHud = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, newHud);
        deathmatchHUD = newHud;

        player.getHudManager().hideHudComponents(playerRef, HudComponent.Mana);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Sleep);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Oxygen);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Compass);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Reticle);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Requests);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.PortalPanel);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Speedometer);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.StatusIcons);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.InputBindings);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BuilderToolsLegend);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.UtilitySlotSelector);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BlockVariantSelector);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BuilderToolsMaterialSlotSelector);
    }
    public static void getLobbyLoot(Player player) {

        player.sendMessage(Message.raw("Cargando loot inicial, mundo: '" + player.getWorld().getName() + "'").color(Color.CYAN));

        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
    public static void getGameLoot(Player player) {
        World playerWorld = player.getWorld();
        if (playerWorld == null) { return; }

        // Get-Item
        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack gun = new ItemStack("Weapon_Handgun", 1);
        ItemStack knife = new ItemStack("Weapon_Daggers_Cobalt", 1);
        ItemStack bullets = new ItemStack("Weapon_Arrow_Crude", 3600);

        inv.getHotbar().addItemStack(gun);
        inv.getHotbar().addItemStack(knife);
        inv.getStorage().addItemStack(bullets);

        inv.setActiveSlot(0, (byte) 0);
    }
}