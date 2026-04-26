package com.thescar.hygunsplugin.ui.hud.core;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optional runtime bridge to the MultipleHUD plugin. Uses reflection to keep
 * Hyguns independent from a hard compile-time dependency.
 */
public final class MultipleHudBridge {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String MULTIPLE_HUD_CLASS = "com.buuz135.mhud.MultipleHUD";

	private static final Object LOCK = new Object();
	private static volatile boolean available;
	@Nullable
	private static Object pluginInstance;
	@Nullable
	private static Method setCustomHudMethod;
	@Nullable
	private static Method hideCustomHudMethod;

	private MultipleHudBridge() {
	}

	public static boolean isAvailable() {
		resolveIfNeeded();
		return available && pluginInstance != null && setCustomHudMethod != null && hideCustomHudMethod != null;
	}

	public static boolean setCustomHud(Player player, PlayerRef playerRef, String key, CustomUIHud hud) {
		if (player == null || playerRef == null || key == null || hud == null) {
			return false;
		}
		if (!isAvailable()) {
			return false;
		}
		try {
			setCustomHudMethod.invoke(pluginInstance, player, playerRef, key, hud);
			return true;
		} catch (Exception t) {
			LOGGER.atWarning().log("MultipleHUD setCustomHud failed: %s", describeFailure(t));
			return false;
		}
	}

	public static boolean hideCustomHud(Player player, PlayerRef playerRef, String key) {
		if (player == null || playerRef == null || key == null) {
			return false;
		}
		if (!isAvailable()) {
			return false;
		}
		try {
			hideCustomHudMethod.invoke(pluginInstance, player, playerRef, key);
			return true;
		} catch (Exception t) {
			LOGGER.atWarning().log("MultipleHUD hideCustomHud failed: %s", describeFailure(t));
			return false;
		}
	}

	private static String describeFailure(Throwable t) {
		if (t == null) {
			return "unknown";
		}
		Throwable root = unwrap(t);
		if (root == t) {
			return t.getClass().getName() + ": " + String.valueOf(t.getMessage());
		}
		return t.getClass().getName() + " -> " + root.getClass().getName() + ": " + String.valueOf(root.getMessage());
	}

	private static Throwable unwrap(Throwable t) {
		Throwable current = t;
		while (true) {
			Throwable next = null;
			if (current instanceof InvocationTargetException invocationTargetException) {
				next = invocationTargetException.getTargetException();
			}
			if (next == null) {
				next = current.getCause();
			}
			if (next == null || next == current) {
				return current;
			}
			current = next;
		}
	}

	public static boolean canUseForPlayer(@Nullable Player player) {
		if (player == null) {
			return false;
		}
		if (!isAvailable()) {
			return false;
		}
		return player.getHudManager() != null;
	}

	private static void resolveIfNeeded() {
		if (available && pluginInstance != null && setCustomHudMethod != null && hideCustomHudMethod != null) {
			return;
		}

		synchronized (LOCK) {
			if (available && pluginInstance != null && setCustomHudMethod != null && hideCustomHudMethod != null) {
				return;
			}

			try {
				Object instance = null;
				Class<?> multipleHudClass = null;
				// First try plugin manager (works even when class loaders are isolated).
				instance = findEnabledMultipleHudPlugin();
				if (instance != null) {
					multipleHudClass = instance.getClass();
				}

				// Fallback: static singleton lookup by known class name.
				if (multipleHudClass == null) {
					multipleHudClass = Class.forName(MULTIPLE_HUD_CLASS);
				}

				try {
					Method getInstance = multipleHudClass.getMethod("getInstance");
					Object singleton = getInstance.invoke(null);
					if (singleton != null) {
						instance = singleton;
					}

				} catch (Throwable ignored) {
					// Fallback below.
				}

				if (instance == null) {
					available = false;
					return;
				}

				Method setMethod = findMethodByShape(instance.getClass(), "setCustomHud", 4);
				Method hideMethod = findMethodByShape(instance.getClass(), "hideCustomHud", 3);
				if (setMethod == null || hideMethod == null) {
					available = false;
					return;
				}

				setMethod.setAccessible(true);
				hideMethod.setAccessible(true);
				pluginInstance = instance;
				setCustomHudMethod = setMethod;
				hideCustomHudMethod = hideMethod;
				available = true;
			} catch (Throwable ignored) {
				available = false;
			}
		}
	}

	@Nullable
	private static Object findEnabledMultipleHudPlugin() {
		try {
			PluginManager manager = PluginManager.get();
			if (manager == null) {
				return null;
			}
			for (PluginBase plugin : manager.getPlugins()) {
				if (plugin == null || !plugin.isEnabled() || plugin.getIdentifier() == null) {
					continue;
				}
				String group = plugin.getIdentifier().getGroup();
				String name = plugin.getIdentifier().getName();
				if (group == null || name == null) {
					continue;
				}
				if (group.equalsIgnoreCase("buuz135") && name.equalsIgnoreCase("multiplehud")) {
					return plugin;
				}
			}

		} catch (Throwable ignored) {
			return null;
		}

		return null;
	}

	@Nullable
	private static Method findMethodByShape(Class<?> owner, String name, int paramCount) {
		if (owner == null || name == null) {
			return null;
		}
		for (Method method : owner.getMethods()) {
			if (!method.getName().equals(name)) {
				continue;
			}
			if (method.getParameterCount() != paramCount) {
				continue;
			}
			return method;
		}

		for (Method method : owner.getDeclaredMethods()) {
			if (!method.getName().equals(name)) {
				continue;
			}
			if (method.getParameterCount() != paramCount) {
				continue;
			}
			return method;
		}

		return null;
	}
}
