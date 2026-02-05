package Tenzinn.Events;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

public class DetectPlayerReady {

    private static Countertale plugin;

    public static void setPlugin(Countertale pluginInstance) { plugin = pluginInstance; }

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getDisplayName();

        if (plugin.hasReceivedStarterKit(playerId)) { return; }

        // Get-Item
        Inventory inv = player.getInventory();
        ItemStack progressStone = new ItemStack("actions_book", 1);

        if (inv.getHotbar().canAddItemStack(progressStone)) { inv.getHotbar().addItemStack(progressStone); }
        else { inv.getStorage().addItemStack(progressStone); }

        plugin.setReceivedStarterKit(playerId);

        player.sendMessage(Message.raw("You received starter kit!"));
    }
}