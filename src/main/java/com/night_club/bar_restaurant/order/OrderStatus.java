package com.night_club.bar_restaurant.order;

public enum OrderStatus {
    DRAFT,       // waiter is still adding items, cart is editable
    SUBMITTED,   // waiter locked the cart, awaiting/pending payment
    PREPARING,   // counter has started making the order (requires paymentStatus = PAID)
    READY,       // ready for collection
    COLLECTED,   // handed to customer, terminal state
    CANCELLED    // terminal state, can happen from DRAFT or SUBMITTED
}