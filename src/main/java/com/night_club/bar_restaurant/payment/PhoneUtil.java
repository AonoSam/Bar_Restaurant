package com.night_club.bar_restaurant.common;

import java.util.regex.Pattern;

/**
 * Single source of truth for Kenyan phone number validation/normalization.
 * Accepts every format a waiter might type or paste in, and normalizes to
 * Daraja's required 254XXXXXXXXX shape.
 *
 * Supported input formats:
 *   07XXXXXXXX   - Safaricom/other, 10 digits starting 07
 *   01XXXXXXXX   - Safaricom/Airtel, 10 digits starting 01
 *   2547XXXXXXXX - already normalized, 12 digits
 *   2541XXXXXXXX - already normalized, 12 digits
 *   +2547XXXXXXXX / +2541XXXXXXXX - with leading plus
 *
 * Centralizing this in one place matters: scattering phone regex/normalization
 * logic across multiple files is exactly what caused the multi-file phone
 * validation bugs on Tronilla. Every place that touches a phone number
 * (STK push, order customerPhone, employee phone) should call this class.
 */
public final class PhoneUtil {

    private static final Pattern LOCAL_FORMAT = Pattern.compile("^0(7|1)\\d{8}$");
    private static final Pattern INTERNATIONAL_FORMAT = Pattern.compile("^254(7|1)\\d{8}$");

    private PhoneUtil() {
    }

    /**
     * Normalizes any accepted input format to 254XXXXXXXXX.
     * Throws IllegalArgumentException if the number doesn't match a known
     * Kenyan mobile format (Safaricom or Airtel, 07/01 prefixes).
     */
    public static String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String cleaned = rawPhone.trim().replace(" ", "").replace("-", "");

        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }

        if (LOCAL_FORMAT.matcher(cleaned).matches()) {
            // 07XXXXXXXX / 01XXXXXXXX -> drop leading 0, prefix 254
            return "254" + cleaned.substring(1);
        }

        if (INTERNATIONAL_FORMAT.matcher(cleaned).matches()) {
            return cleaned;
        }

        throw new IllegalArgumentException(
                "Invalid phone number: " + rawPhone
                        + ". Expected format 07XXXXXXXX, 01XXXXXXXX, or 2547XXXXXXXX/2541XXXXXXXX"
        );
    }

    public static boolean isValid(String rawPhone) {
        try {
            normalize(rawPhone);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}