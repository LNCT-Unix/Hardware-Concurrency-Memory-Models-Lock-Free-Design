package com.example.inventory.ringbuffer;

public class RingBufferInventoryDemo {
    public static void main(String[] args) throws InterruptedException {
        int initialStock = 100_000;
        int threads = 8;
        int purchasesPerThread = 15_000;
        int ringBufferCapacity = 1024;

        long totalAttempts = (long) threads * purchasesPerThread;
        RingBuffer buffer = new RingBuffer(ringBufferCapacity);
        RingBufferInventory inventory = new RingBufferInventory(initialStock);
        long[] results = new long[2];

        Thread consumer = new Thread(() -> {
            long consumed = 0;

            while (consumed < totalAttempts) {
                if (!buffer.poll()) {
                    Thread.yield();
                    continue;
                }

                if (inventory.reserve()) {
                    results[0]++;
                } else {
                    results[1]++;
                }

                consumed++;
            }
        }, "ring-buffer-consumer");

        Thread[] producers = new Thread[threads];

        consumer.start();

        for (int i = 0; i < threads; i++) {
            producers[i] = new Thread(() -> {
                for (int purchase = 0; purchase < purchasesPerThread; purchase++) {
                    while (!buffer.offer()) {
                        Thread.yield();
                    }
                }
            }, "ring-buffer-producer-" + i);
            producers[i].start();
        }

        for (Thread producer : producers) {
            producer.join();
        }

        consumer.join();

        long success = results[0];
        long failed = results[1];
        long finalStock = inventory.getStock();
        long expectedStock = initialStock - success;
        boolean correct = finalStock == expectedStock
                && finalStock >= 0
                && success + failed == totalAttempts;

        System.out.println("=== Ring Buffer Inventory Demo ===");
        System.out.println();
        System.out.println("Initial stock: " + initialStock);
        System.out.println("Threads: " + threads);
        System.out.println("Purchases per thread: " + purchasesPerThread);
        System.out.println("Total purchase attempts: " + totalAttempts);
        System.out.println();
        System.out.println("Successful purchases: " + success);
        System.out.println("Failed purchases: " + failed);
        System.out.println();
        System.out.println("Final stock: " + finalStock);
        System.out.println("Expected stock: " + expectedStock);
        System.out.println();
        System.out.println("Correct: " + correct);
    }
}
