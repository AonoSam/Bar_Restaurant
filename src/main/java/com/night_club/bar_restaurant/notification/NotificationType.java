package com.night_club.bar_restaurant.notification;

public enum NotificationType {
    NEW_PAID_ORDER,
    LOW_STOCK,
    PAYMENT_FAILED,
    // Not yet triggered by anything - there's no Reservations module yet.
    // Included now so the enum, DB schema, and frontend UI don't need
    // rework when that module lands; only the actual trigger is missing.
    RESERVATION_REMINDER
}