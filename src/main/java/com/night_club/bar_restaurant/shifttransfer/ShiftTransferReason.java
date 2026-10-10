package com.night_club.bar_restaurant.shifttransfer;

// There's no shift-schedule concept in this system (no rostered hours to
// check a clock-in/out against), so "only when the working shift is over"
// can't be enforced as a hard rule server-side. This is the compromise:
// the requester declares which case applies, it's stored and visible to a
// manager for oversight, but it's a self-declared reason, not a verified one.
public enum ShiftTransferReason {
    END_OF_SHIFT,
    EMERGENCY
}