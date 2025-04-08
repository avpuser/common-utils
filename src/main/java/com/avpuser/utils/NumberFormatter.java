package com.avpuser.utils;

public class NumberFormatter {
    /**
     * Converts a long number into a human-readable string format with units (K, M, B, T).
     * Example: 1200 -> "1.2K", 1000000 -> "1.0M".
     *
     * @param number The number to format.
     * @return A formatted string representing the number.
     */
    public static String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number); // Return the number as-is if it's less than 1000.
        }

        final String[] units = {"", "K", "M", "B", "T"}; // Units for thousands, millions, billions, etc.
        int unitIndex = 0; // Index to track the current unit.
        double num = number;

        // Divide the number by 1000 until it's less than 1000, incrementing the unit index.
        while (num >= 1000 && unitIndex < units.length - 1) {
            num /= 1000;
            unitIndex++;
        }

        // Format the number to one decimal place and append the appropriate unit.
        return String.format("%.1f%s", num, units[unitIndex]);
    }
}
