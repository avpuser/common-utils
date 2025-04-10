package com.avpuser.telegram_api;

import java.util.Set;

public class MarkdownV2Escaper {

    // Set of special characters that need to be escaped
    private static final Set<Character> SPECIAL_CHARACTERS = Set.of('.', '_', '!', '[', ']', '(', ')', '~',
            '>', '#', '+', '-', '=', '|', '{', '}');

    /**
     * Escapes all special characters in a string for MarkdownV2.
     *
     * @param input the original string
     * @return the string with escaped characters
     */
    public static String escapeMarkdownV2(String input) {
        if (input == null || input.isBlank()) {
            return input; // Return as is if the input is null or empty
        }

        StringBuilder escapedString = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (SPECIAL_CHARACTERS.contains(c)) {
                escapedString.append('\\'); // Add a backslash before the special character
            }
            escapedString.append(c); // Append the character itself
        }

        return escapedString.toString();
    }

}
