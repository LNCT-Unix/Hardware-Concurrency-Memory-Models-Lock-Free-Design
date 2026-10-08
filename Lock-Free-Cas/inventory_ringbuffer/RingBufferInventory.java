package com.example.inventory.ringbuffer;

public class RingBufferInventory {
    private long stock;

    public RingBufferInventory(long initialStock) {
        if (initialStock < 0) {
            throw new IllegalArgumentException("initialStock must be non-negative");
        }
        this.stock = initialStock;
    }

    public boolean reserve() {
        if (stock == 0) {
            return false;
        }

        stock--;
        return true;
    }

    public long getStock() {
        return stock;
    }
}
