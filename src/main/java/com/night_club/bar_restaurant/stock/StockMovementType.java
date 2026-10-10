package com.night_club.bar_restaurant.stock;

public enum StockMovementType {
    SALE,        // negative - decremented by a paid order
    RESTOCK,     // positive - manager received new stock, optionally from a supplier
    ADJUSTMENT,  // positive or negative - manual correction (e.g. stock count fix)
    WASTE        // negative - breakage, spoilage, spillage
}