package Tenzinn.Events;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

public class DetectPlayerReady {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        // Get-Item
        player.getInventory().clear();
        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
}