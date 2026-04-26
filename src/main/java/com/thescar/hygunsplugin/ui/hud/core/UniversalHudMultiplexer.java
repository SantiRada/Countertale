package com.thescar.hygunsplugin.ui.hud.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Field;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Packet-level HUD compositor. Merges third-party CustomHud streams into
 * per-source groups to avoid wiping each other.
 */
public final class UniversalHudMultiplexer {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String ROOT_SELECTOR = "#HygunsHudRoot";
	private static final String LAYER_SELECTOR_PREFIX = "#HygunsHudLayer";
	private static final String INTERNAL_CLASS = "com.thescar.hygunsplugin.ui.hud.core.UniversalHudMultiplexer";
	private static final String OUR_PACKAGE_PREFIX = "com.thescar.hygunsplugin.";
	private static final int STACK_SKIP_ITEMS = 8;

	private static final AtomicInteger NEXT_SLOT = new AtomicInteger(0);
	private static final Map<String, List<HudInfo>> HUD_MAP = new ConcurrentHashMap<>();
	private static final Map<String, Integer> PLAYER_HANDLER_IDENTITIES = new ConcurrentHashMap<>();
	private static final Set<Thread> IGNORE_THREADS = ConcurrentHashMap.newKeySet();
	private static final Set<Integer> EXISTING_GROUPS = ConcurrentHashMap.newKeySet();
	private static final Set<String> PLAYERS_WITH_MASTER_UI = ConcurrentHashMap.newKeySet();
	private static final String DISABLE_PROPERTY = "hyguns.hud.multiplexer.disabled";
	private static volatile boolean registered;
	private static volatile boolean active;

	private UniversalHudMultiplexer() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		synchronized (UniversalHudMultiplexer.class) {
			if (registered) {
				return;
			}
			// Keep automatic by default; allow explicit hard-disable.
			if (Boolean.getBoolean(DISABLE_PROPERTY)) {
				LOGGER.atInfo().log("UniversalHudMultiplexer disabled by -D%s=true", DISABLE_PROPERTY);
				active = false;
				registered = true;
				return;
			}

			HudMultiplexerBackend.Type backend = HudMultiplexerBackend.resolve();
			if (backend != HudMultiplexerBackend.Type.UNIVERSAL) {
				LOGGER.atInfo().log("UniversalHudMultiplexer disabled: selected backend=%s", backend);
				active = false;
				registered = true;
				return;
			}

			PacketAdapters.registerOutbound(UniversalHudMultiplexer::interceptOutbound);
			active = true;
			registered = true;
			LOGGER.atInfo().log("UniversalHudMultiplexer enabled (experimental)");
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static void clearPlayerState(PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		clearPlayerState(playerRef.getUuid().toString());
	}

	public static void clearPlayerState(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return;
		}
		PLAYER_HANDLER_IDENTITIES.remove(playerName);
		PLAYERS_WITH_MASTER_UI.remove(playerName);
		List<HudInfo> infos = HUD_MAP.remove(playerName);
		if (infos == null) {
			return;
		}
		for (HudInfo info : infos) {
			if (info == null) {
				continue;
			}
			EXISTING_GROUPS.remove(info.hudNo);
		}
	}

	private static boolean interceptOutbound(PacketHandler handler, Packet packet) {
		if (HudMultiplexerBackend.resolve() != HudMultiplexerBackend.Type.UNIVERSAL) {
			return false;
		}
		if (!(packet instanceof CustomHud customHud)) {
			return false;
		}
		if (!(handler instanceof GamePacketHandler gameHandler)) {
			return false;
		}
		Thread current = Thread.currentThread();
		if (IGNORE_THREADS.remove(current)) {
			return false;
		}
		PlayerRef playerRef = gameHandler.getPlayerRef();
		if (playerRef == null) {
			return false;
		}
		String playerKey = playerRef.getUuid().toString();
		if (playerKey == null || playerKey.isBlank()) {
			return false;
		}
		try {
			ensureFreshSession(playerKey, gameHandler);
			String sourceKey = detectSourceKey(Thread.currentThread().getStackTrace());
			if (sourceKey == null || sourceKey.isBlank()) {
				return false;
			}
			if (handlePacket(playerKey, sourceKey, customHud.clear, customHud.commands, playerRef)) {
				return true;
			}

		} catch (Exception e) {
			LOGGER.atWarning().log("UniversalHudMultiplexer failed: %s", e.toString());
		}

		return false;
	}

	private static void ensureFreshSession(String playerKey, GamePacketHandler gameHandler) {
		int currentIdentity = System.identityHashCode(gameHandler);
		Integer previous = PLAYER_HANDLER_IDENTITIES.putIfAbsent(playerKey, currentIdentity);
		if (previous == null) {
			return;
		}
		if (previous.intValue() == currentIdentity) {
			return;
		}
		clearPlayerState(playerKey);
		PLAYER_HANDLER_IDENTITIES.put(playerKey, currentIdentity);
	}

	private static boolean handlePacket(String playerKey, String sourceKey, boolean clear, CustomUICommand[] commands,
	                                    PlayerRef playerRef) {
		List<HudInfo> infos = HUD_MAP.computeIfAbsent(playerKey, ignored -> Collections.synchronizedList(new ArrayList<>()));
		HudInfo info = null;
		synchronized (infos) {
			Iterator<HudInfo> iterator = infos.iterator();
			while (iterator.hasNext()) {
				HudInfo existing = iterator.next();
				if (existing == null) {
					continue;
				}
				if (!sourceKey.equals(existing.uiName)) {
					continue;
				}
				info = existing;
				break;
			}

			if (info == null) {
				info = new HudInfo();
				info.hudNo = NEXT_SLOT.getAndAdd(1);
				info.uiName = sourceKey;
				infos.add(info);
			}
		}

		modifyAppendCommands(info, commands, sourceKey);
		List<CustomUICommand[]> commandArrays = new ArrayList<>();
		if (clear) {
			removeHudGroup(info, commandArrays);
			addNewHudGroup(info, commandArrays);
			commandArrays.add(commands);
			update(new RootCompositeHud(playerRef, commandArrays));
			return true;
		}

		addNewHudGroup(info, commandArrays);
		commandArrays.add(commands);
		update(new RootCompositeHud(playerRef, commandArrays));
		return true;
	}

	private static void modifyAppendCommands(HudInfo info, CustomUICommand[] commands, String sourceKey) {
		if (commands == null) {
			return;
		}
		boolean rewriteInline = isOurSource(sourceKey);
		for (CustomUICommand command : commands) {
			if (command == null) {
				continue;
			}
			boolean isAppend = command.type == CustomUICommandType.Append;
			boolean isAppendInline = command.type == CustomUICommandType.AppendInline;
			if (!isAppend && !isAppendInline) {
				continue;
			}
			if (command.text == null) {
				continue;
			}
			if (command.selector != null) {
				continue;
			}
			if (isAppendInline && !rewriteInline) {
				continue;
			}
			command.selector = groupSelector(info.hudNo);
		}
	}

	private static boolean isOurSource(String sourceKey) {
		if (sourceKey == null || sourceKey.isBlank()) {
			return false;
		}
		String normalized = sourceKey.toLowerCase();
		if (normalized.contains("hygunsplugin")) {
			return true;
		}
		return sourceKey.startsWith(OUR_PACKAGE_PREFIX);
	}

	private static void removeHudGroup(HudInfo info, List<CustomUICommand[]> commands) {
		if (!EXISTING_GROUPS.contains(info.hudNo)) {
			return;
		}
		EXISTING_GROUPS.remove(info.hudNo);
		commands.add(generateMasterRemovalCommand(info));
	}

	private static void addNewHudGroup(HudInfo info, List<CustomUICommand[]> commands) {
		if (!EXISTING_GROUPS.add(info.hudNo)) {
			return;
		}
		UICommandBuilder builder = new UICommandBuilder();
		builder.appendInline(ROOT_SELECTOR, "Group " + groupSelector(info.hudNo) + " { Visible: true; }");
		commands.add(builder.getCommands());
	}

	private static void update(RootCompositeHud hud) {
		IGNORE_THREADS.add(Thread.currentThread());
		UICommandBuilder builder = new UICommandBuilder();
		hud.build(builder);
		hud.update(false, builder);
	}

	private static CustomUICommand[] generateMasterRemovalCommand(HudInfo info) {
		UICommandBuilder builder = new UICommandBuilder();
		builder.remove(groupSelector(info.hudNo));
		return builder.getCommands();
	}

	private static String detectSourceKey(StackTraceElement[] stack) {
		if (stack == null) {
			return null;
		}
		String selectedClassName = findSourceClassName(stack, STACK_SKIP_ITEMS);
		if (selectedClassName == null) {
			// First packets on join can have shallow stacks; fall back to unskipped scan.
			selectedClassName = findSourceClassName(stack, 0);
		}

		if (selectedClassName == null || selectedClassName.isBlank()) {
			return null;
		}
		String normalizedClassName = normalizeClassName(selectedClassName);
		if (normalizedClassName == null || normalizedClassName.isBlank()) {
			return null;
		}
		try {
			Class<?> sourceClass = Class.forName(normalizedClassName);
			ProtectionDomain pd = sourceClass.getProtectionDomain();
			if (pd == null) {
				return normalizedClassName;
			}
			CodeSource cs = pd.getCodeSource();
			if (cs == null) {
				return normalizedClassName;
			}
			URL location = cs.getLocation();
			if (location == null) {
				return normalizedClassName;
			}
			return Objects.toString(location.getFile(), normalizedClassName);
		} catch (Throwable ignored) {
			return normalizedClassName;
		}
	}

	private static String findSourceClassName(StackTraceElement[] stack, int skipItems) {
		int skipped = 0;
		for (StackTraceElement element : stack) {
			if (skipped < skipItems) {
				skipped++;
				continue;
			}

			String className = element.getClassName();
			if (className == null) {
				continue;
			}
			if (className.startsWith("com.hypixel.hytale.server.core")) {
				continue;
			}
			if (className.startsWith("java.lang.Thread")) {
				continue;
			}
			if (className.startsWith(INTERNAL_CLASS)) {
				continue;
			}
			return className;
		}

		return null;
	}

	private static String normalizeClassName(String rawClassName) {
		if (rawClassName == null || rawClassName.isBlank()) {
			return rawClassName;
		}
		String className = rawClassName;
		int hiddenClassSeparator = className.indexOf('/');
		if (hiddenClassSeparator > 0) {
			className = className.substring(0, hiddenClassSeparator);
		}

		int lambdaMarker = className.indexOf("$$Lambda");
		if (lambdaMarker > 0) {
			className = className.substring(0, lambdaMarker);
		}

		return className;
	}

	private static String groupSelector(int hudNo) {
		return LAYER_SELECTOR_PREFIX + hudNo;
	}

	private static final class HudInfo {
		private int hudNo;

		private String uiName;
	}

	private static final class RootCompositeHud extends CustomUIHud {
		private final List<CustomUICommand[]> commandArrayList;

		private RootCompositeHud(PlayerRef playerRef, List<CustomUICommand[]> commandArrayList) {
			super(playerRef);
			this.commandArrayList = commandArrayList;
		}

		@Override
		protected void build(UICommandBuilder builder) {
			String playerKey = getPlayerRef().getUuid().toString();
			if (playerKey != null && PLAYERS_WITH_MASTER_UI.add(playerKey)) {
				builder.append("HygunsHudRoot.ui");
			}

			if (commandArrayList == null) {
				return;
			}
			try {
				Field field = UICommandBuilder.class.getDeclaredField("commands");
				field.setAccessible(true);
				@SuppressWarnings("unchecked")
				List<CustomUICommand> existing = (List<CustomUICommand>) field.get(builder);
				for (CustomUICommand[] commandArray : commandArrayList) {
					if (commandArray == null) {
						continue;
					}
					existing.addAll(Arrays.asList(commandArray));
				}

			} catch (Exception e) {
				LOGGER.atWarning().withCause(e).log("Failed to build master HUD");
			}
		}
	}
}
