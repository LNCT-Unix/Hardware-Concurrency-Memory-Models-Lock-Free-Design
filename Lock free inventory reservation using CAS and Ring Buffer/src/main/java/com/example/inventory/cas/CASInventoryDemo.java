package com.example.inventory.cas;

import java.util.concurrent.atomic.LongAdder;

public class CASInventoryDemo {
    public static void main(String[] args) throws InterruptedException {
        int initialStock = 100_000;
        int threads = 8;
        int purchasesPerThread = 15_000;

        long totalAttempts = (long) threads * purchasesPerThread;
        CASInventory inventory = new CASInventory(initialStock);
        LongAdder successfulPurchases = new LongAdder();
        LongAdder failedPurchases = new LongAdder();

        Thread[] workers = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int purchase = 0; purchase < purchasesPerThread; purchase++) {
                    if (inventory.reserve()) {
                        successfulPurchases.increment();
                    } else {
                        failedPurchases.increment();
                    }
                }
            }, "cas-customer-" + i);
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        long success = successfulPurchases.sum();
        long failed = failedPurchases.sum();
        long finalStock = inventory.getStock();
        long expectedStock = initialStock - success;
        boolean correct = finalStock == expectedStock
                && finalStock >= 0
                && success + failed == totalAttempts;

        System.out.println("=== CAS Inventory Demo ===");
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
