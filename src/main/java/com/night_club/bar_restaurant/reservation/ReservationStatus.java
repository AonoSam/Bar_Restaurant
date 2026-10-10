package com.night_club.bar_restaurant.reservation;

public enum ReservationStatus {
    PENDING,     // created, deposit not yet recorded
    CONFIRMED,   // deposit recorded, reservation is locked in
    CANCELLED,
    COMPLETED,   // guest arrived and the reservation ran its course
    NO_SHOW
}