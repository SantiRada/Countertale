package com.thescar.hygunsplugin.support.math;

import javax.annotation.Nullable;

public record NumericComparison(Operator operator, double amount) {
	public static @Nullable NumericComparison parse(@Nullable String raw) {
		if (raw == null) {
			return null;
		}

		String value = raw.trim();
		for (Operator operator : Operator.VALUES) {
			if (!value.startsWith(operator.token())) {
				continue;
			}

			String number = value.substring(operator.token().length()).trim();
			if (number.isEmpty()) {
				return null;
			}

			try {
				return new NumericComparison(operator, Double.parseDouble(number));
			} catch (NumberFormatException ignored) {
				return null;
			}
		}

		return null;
	}

	public boolean test(double value) {
		return this.operator.test(value, this.amount);
	}

	public enum Operator {
		GREATER_OR_EQUAL(">=") {
			@Override
			public boolean test(double left, double right) {
				return left >= right;
			}
		},
		LESS_OR_EQUAL("<=") {
			@Override
			public boolean test(double left, double right) {
				return left <= right;
			}
		},
		GREATER(">") {
			@Override
			public boolean test(double left, double right) {
				return left > right;
			}
		},
		LESS("<") {
			@Override
			public boolean test(double left, double right) {
				return left < right;
			}
		},
		EQUAL("=") {
			@Override
			public boolean test(double left, double right) {
				return Double.compare(left, right) == 0;
			}
		};

		private static final Operator[] VALUES = values();
		private final String token;

		Operator(String token) {
			this.token = token;
		}

		public String token() {
			return this.token;
		}

		public abstract boolean test(double left, double right);
	}
}
