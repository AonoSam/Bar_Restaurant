package com.night_club.bar_restaurant.shifttransfer;

public enum ShiftTransferStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    // Requester can withdraw their own still-pending request - not explicitly
    // asked for, but without it a request nobody responds to sits stuck
    // forever with no way to clean it up or send a new one.
    CANCELLED
}