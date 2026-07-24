package com.srm.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class SensitiveValuePolicy {

    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "CHANGE_ME",
            "CHANGEME",
            "REPLACE_ME",
            "REPLACE_WITH",
            "EXAMPLE_ONLY",
            "EXAMPLE_PASSWORD",
            "YOUR_PASSWORD",
            "YOUR_SECRET",
            "YOUR_TOKEN",
            "PLACEHOLDER");

    private SensitiveValuePolicy() {
    }

    public static String requireSecret(String name, String value, int minimumBytes) {
        String normalized = requirePresent(name, value);
        if (normalized.getBytes(StandardCharsets.UTF_8).length < minimumBytes) {
            throw new IllegalStateException(name + " must contain at least " + minimumBytes + " UTF-8 bytes");
        }
        rejectPlaceholder(name, normalized);
        return normalized;
    }

    public static String requirePassword(String name, String value, int minimumCharacters) {
        String normalized = requirePresent(name, value);
        if (normalized.length() < minimumCharacters) {
            throw new IllegalStateException(name + " must contain at least " + minimumCharacters + " characters");
        }
        rejectPlaceholder(name, normalized);
        long characterClasses = List.of(
                        normalized.chars().anyMatch(Character::isLowerCase),
                        normalized.chars().anyMatch(Character::isUpperCase),
                        normalized.chars().anyMatch(Character::isDigit),
                        normalized.chars().anyMatch(character -> !Character.isLetterOrDigit(character)))
                .stream()
                .filter(Boolean::booleanValue)
                .count();
        if (characterClasses < 3) {
            throw new IllegalStateException(name + " must use at least three character classes");
        }
        return normalized;
    }

    public static void rejectMatchingPasswords(String firstName, String first, String secondName, String second) {
        if (first.equals(second)) {
            throw new IllegalStateException(firstName + " and " + secondName + " must differ");
        }
    }

    private static String requirePresent(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalStateException(name + " must not contain leading or trailing whitespace");
        }
        return value;
    }

    private static void rejectPlaceholder(String name, String value) {
        String upper = value.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (value.indexOf('<') >= 0
                || value.indexOf('>') >= 0
                || PLACEHOLDER_MARKERS.stream().anyMatch(upper::contains)) {
            throw new IllegalStateException(name + " must not use an example or placeholder value");
        }
    }
}
