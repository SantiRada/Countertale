package com.thescar.hygunsplugin.debug;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public final class DebugLogger {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private DebugLogger() {
	}

	public static boolean isEnabled() {
		return DebugSettings.isEnabled();
	}

	public static void debug(@Nonnull String tag, @Nonnull Supplier<String> messageSupplier) {
		if (!isEnabled()) {
			return;
		}

		LOGGER.atInfo().log("[Debug][%s] %s", tag, messageSupplier.get());
	}

	public static void debug(@Nonnull String tag, @Nonnull String message) {
		if (!isEnabled()) {
			return;
		}

		LOGGER.atInfo().log("[Debug][%s] %s", tag, message);
	}
}
