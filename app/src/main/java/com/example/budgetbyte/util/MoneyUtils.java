package com.example.budgetbyte.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Utility methods for amounts persisted as minor currency units. */
public final class MoneyUtils {
    private static final BigDecimal MINOR_UNIT_SCALE = BigDecimal.valueOf(100L);

    private MoneyUtils() { }

    /**
     * Parses a string representing a currency amount into persisted minor units (e.g. "12.50" -> 1250L).
     * Accepts:
     * - Western digits (0-9)
     * - Arabic-Indic digits (٠-٩)
     * - Eastern Arabic-Indic digits (۰-۹)
     * - Decimal dots (.) and commas (,), as well as Arabic decimal separator (٫, \u066B)
     * - Thousands grouping commas and dots, as well as Arabic thousands separator (٬, \u066C)
     * - Optional currency labels (EGP, ج.م, $, etc.)
     *
     * @param input the raw user or OCR string
     * @return the amount in minor units (0L if empty/null)
     * @throws IllegalArgumentException if the input is malformed, negative, or exceeds bounds
     */
    public static long parseMinorUnits(String input) {
        if (input == null) {
            return 0L;
        }
        String normalized = normalizeDigits(input.trim());
        if (normalized.isEmpty()) {
            return 0L;
        }

        // Remove common currency symbols and labels
        normalized = normalized.replaceAll("(?i)(EGP|ج\\.م|LE|L\\.E\\.|USD|EUR|\\$|€|£)", "").trim();

        // Remove whitespace
        normalized = normalized.replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return 0L;
        }

        // Identify decimal separator vs thousands separators
        if (normalized.contains(".") && normalized.contains(",")) {
            int lastDot = normalized.lastIndexOf('.');
            int lastComma = normalized.lastIndexOf(',');
            if (lastDot > lastComma) {
                // Comma is thousands, dot is decimal (e.g., 1,250.50)
                String wholePart = normalized.substring(0, lastDot).replace(",", "");
                String fractionPart = normalized.substring(lastDot + 1);
                normalized = wholePart + "." + fractionPart;
            } else {
                // Dot is thousands, comma is decimal (e.g., 1.250,50)
                String wholePart = normalized.substring(0, lastComma).replace(".", "");
                String fractionPart = normalized.substring(lastComma + 1);
                normalized = wholePart + "." + fractionPart;
            }
        } else if (normalized.contains(",")) {
            int lastComma = normalized.lastIndexOf(',');
            int fractionLength = normalized.length() - lastComma - 1;
            if (fractionLength >= 1 && fractionLength <= 2) {
                String wholePart = normalized.substring(0, lastComma).replace(",", "");
                String fractionPart = normalized.substring(lastComma + 1);
                normalized = wholePart + "." + fractionPart;
            } else if (fractionLength == 3) {
                normalized = normalized.replace(",", "");
            } else {
                throw new IllegalArgumentException("Invalid comma placement: " + input);
            }
        } else if (normalized.contains(".")) {
            int lastDot = normalized.lastIndexOf('.');
            int fractionLength = normalized.length() - lastDot - 1;
            if (fractionLength >= 1 && fractionLength <= 2) {
                String wholePart = normalized.substring(0, lastDot).replace(".", "");
                String fractionPart = normalized.substring(lastDot + 1);
                normalized = wholePart + "." + fractionPart;
            } else {
                throw new IllegalArgumentException("Invalid decimal fraction: " + input);
            }
        }

        if (!normalized.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Invalid amount format: " + input);
        }

        try {
            return new BigDecimal(normalized)
                    .movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Amount out of range: " + input, e);
        }
    }

    /** Formats a persisted minor-unit amount as a decimal number without currency symbol (e.g. 8,250.00). */
    public static String formatNumber(long minorUnits) {
        long absoluteMinorUnits = Math.abs(minorUnits);
        long units = absoluteMinorUnits / 100L;
        long cents = absoluteMinorUnits % 100L;
        String sign = minorUnits < 0 ? "-" : "";
        return String.format(Locale.US, "%s%,d.%02d", sign, units, cents);
    }

    /** Normalizes Arabic-Indic and Eastern Arabic-Indic numerals and separators to standard ASCII. */
    public static String normalizeDigits(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + c - '\u0660'));
            } else if (c >= '\u06F0' && c <= '\u06F9') {
                sb.append((char) ('0' + c - '\u06F0'));
            } else if (c == '\u066B') {
                sb.append('.');
            } else if (c == '\u066C') {
                sb.append(',');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Converts a major-unit amount, such as 10.50, to persisted minor units (1050). */
    public static long toMinorUnits(double amount) {
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Amount must be finite");
        }
        return BigDecimal.valueOf(amount)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Converts persisted minor units, such as 1050, to a major-unit amount (10.50). */
    public static double toMajorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits).divide(MINOR_UNIT_SCALE).doubleValue();
    }

    /** Convenience overload for the default currency. */
    public static String formatAmount(long minorUnits) {
        return formatAmount(minorUnits, "EGP");
    }

    /** Formats a persisted minor-unit amount using integer division to avoid floating-point display math. */
    public static String formatAmount(long minorUnits, String currencyCode) {
        String code = currencyCode == null || currencyCode.trim().isEmpty()
                ? "EGP" : currencyCode.trim().toUpperCase(Locale.ROOT);

        long absoluteMinorUnits = Math.abs(minorUnits);
        long units = absoluteMinorUnits / 100L;
        long cents = absoluteMinorUnits % 100L;
        String sign = minorUnits < 0 ? "-" : "";

        return String.format(Locale.US, "%s%d.%02d %s", sign, units, cents, code);
    }
}
