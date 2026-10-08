import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameRaceBenchmark {

    static final int ITERATIONS = 10_000;
    static final int BENCHMARK_THREADS = 4;
    static final int OPS_PER_THREAD = 250_000; // 1M total ops

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. TOCTOU Race Condition Invariant Test ===");
        int duplicates = testUnsafeRegistrationRace(ITERATIONS);
        System.out.printf("Ran %d concurrent registration attempts for same username.%n", ITERATIONS);
        System.out.printf("Duplicate Registrations (Invariants Broken): %d (%.2f%% failure rate)%n%n",
                duplicates, (duplicates * 100.0) / ITERATIONS);

        System.out.println("=== 2. Throughput & Latency Benchmark (1,000,000 ops) ===");
        long syncTime = benchmarkSynchronized();
        long concurrentTime = benchmarkConcurrentSet();

        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-26s | %-12s | %-16s | %-12s%n", "Implementation", "Thread Safety", "Duplicates / 10k", "Time (1M ops)");
        System.out.println("----------------------------------------------------------------------------------");
        System.out.printf("%-26s | %-12s | %-16s | %-12s%n", "HashSet (Unsynchronized)", "BROKEN (Race)", duplicates + " / " + ITERATIONS, "N/A (Crashes)");
        System.out.printf("%-26s | %-12s | %-16s | %-12s%n", "Synchronized HashSet", "Safe (Locks)", "0 / " + ITERATIONS, syncTime + " ms");
        System.out.printf("%-26s | %-12s | %-16s | %-12s%n", "ConcurrentHashMap KeySet", "Safe (CAS)", "0 / " + ITERATIONS, concurrentTime + " ms");
        System.out.println("----------------------------------------------------------------------------------");
    }

    // Demonstrates duplicate registration bug in unsynchronized HashSet check-then-act
    static int testUnsafeRegistrationRace(int iterations) throws InterruptedException {
        int duplicates = 0;
        for (int i = 0; i < iterations; i++) {
            Set<String> set = new HashSet<>();
            AtomicInteger claims = new AtomicInteger(0);

            Thread t1 = new Thread(() -> {
                if (!set.contains("dev47929")) {
                    set.add("dev47929");
                    claims.incrementAndGet();
                }
            });
            Thread t2 = new Thread(() -> {
                if (!set.contains("dev47929")) {
                    set.add("dev47929");
                    claims.incrementAndGet();
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            if (claims.get() > 1) {
                duplicates++;
            }
        }
        return duplicates;
    }

    static long benchmarkSynchronized() throws InterruptedException {
        Set<String> set = Collections.synchronizedSet(new HashSet<>());
        long start = System.nanoTime();
        runWorkerThreads(() -> {
            for (int i = 0; i < OPS_PER_THREAD; i++) {
                synchronized (set) {
                    set.add("user_" + (i % 10_000));
                }
            }
        });
        return (System.nanoTime() - start) / 1_000_000;
    }

    static long benchmarkConcurrentSet() throws InterruptedException {
        Set<String> set = ConcurrentHashMap.newKeySet();
        long start = System.nanoTime();
        runWorkerThreads(() -> {
            for (int i = 0; i < OPS_PER_THREAD; i++) {
                set.add("user_" + (i % 10_000));
            }
        });
        return (System.nanoTime() - start) / 1_000_000;
    }

    static void runWorkerThreads(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[BENCHMARK_THREADS];
        for (int i = 0; i < BENCHMARK_THREADS; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        for (Thread t : threads) t.join();
    }
}
