package com.example.inventory.ringbuffer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class RingBuffer {
    private static final Object REQUEST_EVENT = new Object();

    private final AtomicReferenceArray<Object> entries;
    private final int capacity;
    private final AtomicLong head = new AtomicLong();
    private final AtomicLong tail = new AtomicLong();

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.entries = new AtomicReferenceArray<>(capacity);
    }

    public boolean offer() {
        while (true) {
            long currentTail = tail.get();
            long currentHead = head.get();

            if (currentTail - currentHead >= capacity) {
                return false;
            }

            if (tail.compareAndSet(currentTail, currentTail + 1)) {
                entries.set(index(currentTail), REQUEST_EVENT);
                return true;
            }
        }
    }

    public boolean poll() {
        long currentHead = head.get();

        if (currentHead >= tail.get()) {
            return false;
        }

        int index = index(currentHead);
        Object request = entries.get(index);

        if (request == null) {
            return false;
        }

        entries.set(index, null);
        head.set(currentHead + 1);
        return true;
    }

    private int index(long sequence) {
        return (int) (sequence % capacity);
    }
}
