package Tenzinn;

import Tenzinn.Admin.UI.ServerStatusHud;
import Tenzinn.Admin.Commands.ServerStatusCommand;
import Tenzinn.Admin.Commands.HideServerStatusCommand;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.Map;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Countertale extends JavaPlugin {

    private final Map<String, ServerStatusHud> activeHuds = new ConcurrentHashMap<>();
    private ScheduledFuture<?> updateTask;

    private ServerStatusHud hud;

    public Countertale(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new ServerStatusCommand("server", "Show server status", this));
        getCommandRegistry().registerCommand(new HideServerStatusCommand("hide", "Hide server status HUD", this));
    }

    @Override
    protected void start() {
        updateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::updateAllHuds,1,1,TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> taskVoid = (ScheduledFuture<Void>) updateTask;
        getTaskRegistry().registerTask(taskVoid);
    }
    private void updateAllHuds() { activeHuds.values().forEach(ServerStatusHud::updateStats); }
    public void registerHud(String playerId, ServerStatusHud hud) { activeHuds.put(playerId, hud); this.hud = hud; }
    public void unregisterHud(String playerId) { activeHuds.remove(playerId); hud.hideStats(); }
    public boolean hasActiveHud(String playerId) { return activeHuds.containsKey(playerId); }
}