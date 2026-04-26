package com.thescar.hygunsplugin.content.settings;

import com.hypixel.hytale.codec.Codec;

public enum CodecKind {
	BOOLEAN(Codec.BOOLEAN),
	INTEGER(Codec.INTEGER),
	DOUBLE(Codec.DOUBLE),
	STRING(Codec.STRING);
	private final Codec<?> codec;

	CodecKind(Codec<?> codec) {
		this.codec = codec;
	}

	public Codec<?> codec() {
		return this.codec;
	}
}
