package com.night_club.bar_restaurant.order;

public enum PaymentStatus {
    UNPAID,
    PENDING,  // STK push sent, awaiting Daraja callback
    PAID,
    FAILED
}