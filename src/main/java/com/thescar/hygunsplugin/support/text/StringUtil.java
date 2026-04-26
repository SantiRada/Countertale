package com.thescar.hygunsplugin.support.text;

import javax.annotation.Nullable;

public class StringUtil {
	public static String leftPad(String value, int width, char padChar) {
		if (value == null) {
			value = "";
		}
		if (value.length() >= width) {
			return value;
		}
		return String.valueOf(padChar).repeat(width - value.length()) + value;
	}

	public static @Nullable String normalize(@Nullable String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty()
		       ? null
		       : trimmed;
	}
}
