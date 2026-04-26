package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonElement;

import javax.annotation.Nullable;

public enum JsonReadKind {
	BOOLEAN {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.bool(element);
		}

	},
	INTEGER_POSITIVE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.positiveInt(element);
		}

	},
	INTEGER_NON_NEGATIVE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.nonNegativeInt(element);
		}

	},
	DOUBLE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.dbl(element);
		}

	},
	DOUBLE_POSITIVE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.positiveDouble(element);
		}

	},
	DOUBLE_NON_NEGATIVE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.nonNegativeDouble(element);
		}

	},
	DOUBLE_ZERO_TO_ONE {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.zeroToOneDouble(element);
		}

	},
	STRING_NON_BLANK {
		@Override
		public @Nullable Object read(@Nullable JsonElement element) {
			return JsonValueUtils.Read.nonBlankString(element);
		}

	};

	public abstract @Nullable Object read(@Nullable JsonElement element);
}
