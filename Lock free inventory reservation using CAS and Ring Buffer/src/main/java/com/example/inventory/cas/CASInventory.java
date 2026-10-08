package com.example.inventory.cas;

import java.util.concurrent.atomic.AtomicLong;

public class CASInventory {
    private final AtomicLong stock;

    public CASInventory(long initialStock) {
        if (initialStock < 0) {
            throw new IllegalArgumentException("initialStock must be non-negative");
        }
        this.stock = new AtomicLong(initialStock);
    }

    public boolean reserve() {
        long current = stock.get();

        while (current > 0) {
            long updated = current - 1;
            if (stock.compareAndSet(current, updated)) {
                return true;
            }

            current = stock.get();
        }

        return false;
    }

    public long getStock() {
        return stock.get();
    }
}
