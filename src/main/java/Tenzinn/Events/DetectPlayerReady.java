package Tenzinn.Events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.awt.*;

public class DetectPlayerReady {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        if(player.getWorld().getName().equals("default")) { getLobbyLoot(player); }
    }
    public static void getLobbyLoot(Player player) {

        player.sendMessage(Message.raw("Cargando loot inicial, mundo: '" + player.getWorld().getName() + "'").color(Color.CYAN));

        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
}