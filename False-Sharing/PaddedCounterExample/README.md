# False Sharing: Benchmark & Testing

## 1. Overview & Hardware Mechanics
In multi-core CPU architectures, memory is transferred to L1/L2 caches in **64-byte chunks (cache lines)**. **False Sharing** occurs when independent threads concurrently modify distinct variables that share the same 64-byte cache line:

```java
// Unpadded: x and y reside in the same 64-byte cache line -> MESI invalidation storm
volatile long x = 0;
volatile long y = 0;

// Padded: 64-byte padding (8 longs * 8 bytes) isolates x and y onto separate cache lines
volatile long x = 0;
long p1, p2, p3, p4, p5, p6, p7, p8; // Cache line padding
volatile long y = 0;
```

When Thread 1 mutates `x` and Thread 2 mutates `y`, the **MESI cache coherence protocol** continuously invalidates each core's L1 cache line, causing bus contention and pipeline stalls despite zero logical data dependency. Adding 64 bytes of padding isolates each variable onto a separate cache line.

---

## 2. Benchmark & Concurrency Test Results

Testing was conducted using [`PaddedCounterBenchmark.java`](file:///e:/1.Code/ResumeProject1/Hardware-Concurrency-Memory-Models-Lock-Free-Design/False-Sharing/PaddedCounterExample/PaddedCounterBenchmark.java) across 2 concurrent worker threads executing **100,000,000 increments** per variable on Java 21 LTS:

| Implementation | Cache Line Layout | Invariant Test (100M Count) | Execution Time | Speedup | Hardware Cache Impact |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **[`UnpaddedCounter`](file:///e:/1.Code/ResumeProject1/Hardware-Concurrency-Memory-Models-Lock-Free-Design/False-Sharing/PaddedCounterExample/UnpaddedCounter.java)** | Shared line (`x` & `y` contiguous) | **PASSED** (100M each) | **~1,768 ms** | 1.00x (Baseline) | Continuous MESI cache line invalidation ping-pong |
| **[`PaddedCounter`](file:///e:/1.Code/ResumeProject1/Hardware-Concurrency-Memory-Models-Lock-Free-Design/False-Sharing/PaddedCounterExample/PaddedCounter.java)** | Isolated via 64-byte padding | **PASSED** (100M each) | **~151 ms** | **~11.71x faster** | Independent cache lines, zero false sharing (L1 hit) |

> **Key Takeaway:** Cache line padding delivers an **~11.7x throughput improvement (~91.5% latency reduction)** purely by aligning data structures to hardware cache line boundaries without altering business logic.

---

## 3. How to Run & Verify

### Run the Benchmark & Invariant Test Suite:
```powershell
javac False-Sharing/PaddedCounterExample/PaddedCounterBenchmark.java
java -cp False-Sharing PaddedCounterExample.PaddedCounterBenchmark
```

### Run the Individual Baseline Demonstrations:
```powershell
# Unpadded (demonstrates false sharing bottleneck)
javac False-Sharing/PaddedCounterExample/UnpaddedCounter.java
java -cp False-Sharing PaddedCounterExample.UnpaddedCounter

# Padded (demonstrates false sharing mitigation)
javac False-Sharing/PaddedCounterExample/PaddedCounter.java
java -cp False-Sharing PaddedCounterExample.PaddedCounter
```
