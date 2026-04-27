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

    private static final int TIME_PER_ROUND    = 600;
    private static final int TIME_PER_PURCHASE = 15;

    private static GameMatch myMatch;

    public static void startTimerMatch(GameMatch match) {
        if (match == null || match.getState() == GameMatch.MatchState.FINISHED) return;

        myMatch = match;
        if (timerTask != null && !timerTask.isDone()) return;

        if (myMatch.getState() == GameMatch.MatchState.STARTING) {
            remainingSeconds = TIME_PER_PURCHASE;

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0) {
                    onEndPurchase();
                    return;
                }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        } else {
            myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
            remainingSeconds = TIME_PER_ROUND;

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0) {
                    onEndMatch();
                    return;
                }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    public static void onEndPurchase() {
        if (myMatch == null || myMatch.getState() == GameMatch.MatchState.FINISHED) {
            stopTimer();
            return;
        }

        stopTimer();
        myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
        startTimerMatch(myMatch);
    }

    public static void onEndMatch() {
        if (myMatch == null || myMatch.getState() == GameMatch.MatchState.FINISHED) {
            stopTimer();
            return;
        }

        if (myMatch.getPlayers().isEmpty()) {
            stopTimer();
            myMatch.setState(GameMatch.MatchState.FINISHED);
            return;
        }

        remainingSeconds = 0;
        stopTimer();
        myMatch.setState(GameMatch.MatchState.FINISHED);

        World world = Universe.get().getWorld(
                Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));

        if (world == null) return;

        GameMatch finishedMatch = myMatch;

        world.execute(() -> RefactorTool.finishGame(finishedMatch.getPlayers(), finishedMatch));
    }

    public static int getTimer()  { return remainingSeconds; }
    public static void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
        }

        timerTask = null;
    }
}
