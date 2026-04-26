package com.thescar.hygunsplugin.ui.hud.core;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.ui.hud.scope.EmptyHud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class HudScreenRuntime {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final HudScreenRuntime INSTANCE = new HudScreenRuntime();
	private static final String MULTIPLE_HUD_KEY = HygunsPluginMain.PACK_PREFIX;
	private final Map<String, HudScreenContract<?>> contracts = new ConcurrentHashMap<>();
	private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

	private HudScreenRuntime() {
	}

	public static HudScreenRuntime get() {
		return INSTANCE;
	}

	@SuppressWarnings("unchecked")
	private static <T> HudScreenStackHud.RenderEntry<T> buildEntry(HudScreenContract<?> rawContract, Object rawState) {
		HudScreenContract<T> contract = (HudScreenContract<T>) rawContract;
		T state = (T) rawState;
		return new HudScreenStackHud.RenderEntry<>(contract, state);
	}

	public void register(HudScreenContract<?> contract) {
		if (contract == null) {
			return;
		}
		contracts.put(contract.id(), contract);
	}

	public void attach(PlayerRef playerRef, Player player) {
		attach(playerRef, player, player.getWorld());
	}

	public void attach(PlayerRef playerRef, Player player, @Nullable World world) {
		if (playerRef == null || player == null) {
			return;
		}
		sessions.compute(playerRef.getUuid(), (uuid, old) -> new Session(playerRef, player, world));
	}

	@Nullable
	public Session getSession(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		return sessions.get(playerId);
	}

	public void detach(UUID playerId) {
		if (playerId == null) {
			return;
		}
		sessions.remove(playerId);
	}

	public boolean hasExternalHudConflict(Player player) {
		HudMultiplexerBackend.Type backend = HudMultiplexerBackend.resolve();
		if (backend == HudMultiplexerBackend.Type.MULTIPLE_HUD) {
			return false;
		}
		if (backend == HudMultiplexerBackend.Type.AUTO_MULTI_HUD) {
			return false;
		}
		if (backend == HudMultiplexerBackend.Type.UNIVERSAL && UniversalHudMultiplexer.isActive()) {
			return false;
		}
		if (player == null || player.getHudManager() == null) {
			return false;
		}
		CustomUIHud customHud = player.getHudManager().getCustomHud();
		if (customHud == null) {
			return false;
		}
		if (customHud instanceof HudScreenStackHud) {
			return false;
		}
		String customHudClass = customHud.getClass().getName();
		if (customHudClass.startsWith("com.dairymoose.auto_multi_hud.")) {
			return false;
		}
		if (customHudClass.startsWith("com.buuz135.mhud.")) {
			return false;
		}
		return !customHud.getClass().getName().startsWith("com.thescar.hygunsplugin.hud.");
	}

	public void show(PlayerRef playerRef, String screenId) {
		Session session = session(playerRef);
		if (session == null) {
			return;
		}
		synchronized (session) {
			if (session.visibleScreens.add(screenId)) {
				rebuild(session);
			}
		}
	}

	public void hide(PlayerRef playerRef, String screenId) {
		Session session = session(playerRef);
		if (session == null) {
			return;
		}
		synchronized (session) {
			if (session.visibleScreens.remove(screenId)) {
				rebuild(session);
			}
		}
	}

	public <T> void setState(PlayerRef playerRef, String screenId, @Nullable T nextState) {
		Session session = session(playerRef);
		if (session == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		HudScreenContract<T> contract = (HudScreenContract<T>) contracts.get(screenId);
		if (contract == null) {
			return;
		}
		if (nextState != null && !contract.stateType().isInstance(nextState)) {
			return;
		}

		synchronized (session) {
			@SuppressWarnings("unchecked")
			T previousState = (T) session.states.put(screenId, nextState);
			if (!session.visibleScreens.contains(screenId)) {
				return;
			}

			// MultipleHUD wraps child HUDs and applies selector prefixing internally.
			// Sending direct incremental updates from the child HUD can produce invalid
			// selectors on client.
			// For this mode, always rebuild through MultipleHUD integration.
			if (session.usingMultipleHud) {
				rebuild(session);
				return;
			}

			if (!(session.currentHud instanceof HudScreenStackHud stackHud)) {
				rebuild(session);
				return;
			}

			UICommandBuilder b = new UICommandBuilder();
			boolean patched = contract.patch(previousState, nextState, b);
			if (patched) {
				stackHud.update(false, b);
				return;
			}

			rebuild(session);
		}
	}

	public void clear() {
		sessions.clear();
		contracts.clear();
	}

	public boolean isScreenVisible(PlayerRef playerRef, String screenId) {
		if (playerRef == null || screenId == null || screenId.isBlank()) {
			return false;
		}
		Session session = session(playerRef);
		if (session == null) {
			return false;
		}
		synchronized (session) {
			return session.visibleScreens.contains(screenId);
		}
	}

	public boolean hasAnyVisibleScreens(PlayerRef playerRef) {
		if (playerRef == null) {
			return false;
		}
		Session session = session(playerRef);
		if (session == null) {
			return false;
		}
		synchronized (session) {
			return !session.visibleScreens.isEmpty();
		}
	}

	@Nullable
	private Session session(PlayerRef playerRef) {
		if (playerRef == null) {
			return null;
		}
		return sessions.get(playerRef.getUuid());
	}

	private void rebuild(Session session) {
		if (session.player == null || session.playerRef == null) {
			return;
		}
		List<HudScreenContract<?>> active = new ArrayList<>();
		for (String id : session.visibleScreens) {
			HudScreenContract<?> contract = contracts.get(id);
			if (contract != null) {
				active.add(contract);
			}
		}

		active.sort(Comparator.comparingInt(HudScreenContract::zIndex));
		if (active.isEmpty()) {
			if (session.usingMultipleHud) {
				MultipleHudBridge.hideCustomHud(session.player, session.playerRef, MULTIPLE_HUD_KEY);
				session.usingMultipleHud = false;
				session.currentHud = null;
			} else {
				// Do not push EmptyHud before we have ever shown our HUD in this session.
				// This avoids replacing another mod's startup HUD during join races.
				if (session.currentHud != null) {
					session.player.getHudManager().setCustomHud(session.playerRef, new EmptyHud(session.playerRef));
					session.currentHud = session.player.getHudManager().getCustomHud();
				}
			}

			return;
		}

		List<HudScreenStackHud.RenderEntry<?>> entries = new ArrayList<>(active.size());
		for (HudScreenContract<?> contract : active) {
			entries.add(buildEntry(contract, session.states.get(contract.id())));
		}

		HudScreenStackHud hud = new HudScreenStackHud(session.playerRef, entries);
		HudMultiplexerBackend.Type backend = HudMultiplexerBackend.resolve();
		if (backend == HudMultiplexerBackend.Type.MULTIPLE_HUD && !session.multipleHudBridgeFailed) {
			if (!MultipleHudBridge.setCustomHud(session.player, session.playerRef, MULTIPLE_HUD_KEY, hud)) {
				LOGGER.atWarning().log(
					"MULTIPLE_HUD backend selected but setCustomHud failed for %s (bridgeAvailable=%s); falling back to direct CustomUI for this session",
					session.playerRef.getUuid(), MultipleHudBridge.isAvailable()
				);
				session.multipleHudBridgeFailed = true;
				session.player.getHudManager().setCustomHud(session.playerRef, hud);
				session.currentHud = hud;
				session.usingMultipleHud = false;
				return;
			}

			session.currentHud = hud;
			session.usingMultipleHud = true;
			return;
		}

		// AUTO_MULTI_HUD and UNIVERSAL both consume regular CustomHud stream.
		session.player.getHudManager().setCustomHud(session.playerRef, hud);
		session.currentHud = hud;
		session.usingMultipleHud = false;
	}

	public static final class Session {
		private final PlayerRef playerRef;

		private final Player player;
		private final Executor worldExecutor;
		private final Set<String> visibleScreens = new HashSet<>();
		private final Map<String, Object> states = new HashMap<>();
		@Nullable
		private CustomUIHud currentHud;
		private boolean usingMultipleHud;
		private boolean multipleHudBridgeFailed;

		private Session(PlayerRef playerRef, Player player, @Nullable World world) {
			this.playerRef = playerRef;
			this.player = player;
			this.worldExecutor = world != null ? world : player.getWorld();
			this.usingMultipleHud = false;
		}

		public PlayerRef getPlayerRef() {
			return this.playerRef;
		}

		public Player getPlayer() {
			return this.player;
		}

		public void runOnWorldThread(Runnable task) {
			if (task == null) {
				return;
			}
			try {
				worldExecutor.execute(task);
			} catch (RuntimeException t) {
				task.run();
			}
		}
	}
}
