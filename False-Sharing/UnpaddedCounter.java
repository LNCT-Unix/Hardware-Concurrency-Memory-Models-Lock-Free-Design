public class UnpaddedCounter {
static class Counters {
        volatile long x = 0;
        volatile long y = 0;
    }
    public static void main(String[] args) throws InterruptedException {
        Counters counters = new Counters();
        Thread t1 = new Thread(() -> {
            for (long i = 0; i < 100_000_000; i++) {
                counters.x++;
            }
        });
        Thread t2 = new Thread(() -> {
            for (long i = 0; i < 100_000_000; i++) {
                counters.y++;
            }
        });
        long start = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long end = System.nanoTime();
        System.out.println("Time: " + (end - start) / 1_000_000 + " ms");
    }
}