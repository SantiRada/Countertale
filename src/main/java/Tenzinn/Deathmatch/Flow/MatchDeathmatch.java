package Tenzinn.Deathmatch.Flow;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class MatchDeathmatch {

    private static ScheduledFuture<?> timerTask;
    private static int remainingSeconds = 150;

    private static int timePerRound = 600;
    private static int timePerPurchase = 15;

    private static GameMatch myMatch;

    public static void startTimerMatch (GameMatch match) {
        myMatch = match;
        if (timerTask != null && !timerTask.isDone()) return;

        if (myMatch.getState() == GameMatch.MatchState.STARTING) {
            remainingSeconds = timePerPurchase;
            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {

                if (remainingSeconds <= 0) { onEndPurchase(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
        else {
            remainingSeconds = timePerRound;

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {

                if (remainingSeconds <= 0) { onEndMatch(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }
    // ================================================== //
    public static void onEndPurchase() {
        stopTimer();

        myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
        startTimerMatch(myMatch);
    }
    public static void onEndMatch() {
        World world = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
        assert world != null;
        world.execute(() -> { RefactorTool.finishGame(myMatch.getPlayers()); });
    }
    // ================================================== //
    public static int getTimer () { return remainingSeconds; }
    public static void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }
}