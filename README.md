# Hardware Concurrency, Memory Models & Lock-Free Design

[![Java](https://img.shields.io/badge/Java-21%2B%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Concurrency](https://img.shields.io/badge/Concurrency-Lock--Free%20%7C%20CAS-blue?style=for-the-badge)](https://en.wikipedia.org/wiki/Non-blocking_algorithm)
[![Hardware](https://img.shields.io/badge/Hardware-Cache%20Coherence%20%7C%20MESI-brightgreen?style=for-the-badge)](https://en.wikipedia.org/wiki/False_sharing)

A hands-on, hardware-aware exploration of multi-core concurrency, low-level memory models, cache coherence protocols (MESI/MOESI), false sharing mitigation, and lock-free data structures using Compare-And-Swap (CAS) atomics.

This repository demonstrates how software design decisions directly interface with modern CPU microarchitectures (L1/L2/L3 caches, 64-byte cache lines, memory buses, and hardware atomics).

---

## Repository Structure

```text
Hardware-Concurrency-Memory-Models-Lock-Free-Design/
├── README.md                                    # Root project documentation, architecture & benchmarks
├── .gitignore                                   # Git ignore rules for build artifacts and IDE files
│
├── False-Sharing/                               # Cache line false sharing & padding benchmarks
│   ├── Docs.md                                  # Architectural overview of false sharing & MESI protocol
│   ├── UnpaddedCounter.java                     # Contended counters on same 64-byte cache line
│   └── PaddedCounter.java                       # 64-byte padded counters eliminating false sharing
│
├── Lock-Free-Cas/                               # Lock-free algorithms and non-blocking synchronization
│   ├── Docs.md                                  # Theoretical background on CAS & circular ring buffers
│   └── inventory_ringbuffer/                    # High-throughput lock-free inventory ring buffer
│       ├── RingBuffer.java                      # Non-blocking ring buffer using AtomicLong & AtomicReferenceArray
│       ├── RingBufferInventory.java             # Core inventory stock reservation state
│       └── RingBufferInventoryDemo.java         # Multi-producer single-consumer concurrency test harness
│
└── Race-Condition/                              # Concurrency anomalies and synchronization patterns
    ├── Docs.md                                  # Analysis of data races vs. race conditions
    ├── Data-Race/                               # Data races on shared mutable state
    │   ├── IrctcExample.java                    # Unsynchronized ticket booking demonstrating race condition
    │   └── IrctcExampleFixed.java               # Synchronized mutex-locked ticket booking
    ├── Race-condition/                          # TOCTOU & check-then-act stock anomalies
    │   └── Main.java                            # Concurrent pizza shop stock ordering race simulation
    └── UsernameValidation-Race/                 # Check-then-act race on unsynchronized collections
        ├── UsernameRaceInTeamUnix.java          # Concurrent HashSet registration race condition
        └── Output.png                           # Execution screenshot demonstrating duplicate registration
```

---

## 1. False Sharing & Cache Coherence (`False-Sharing/`)

### The Hardware Problem
Modern CPUs process data in chunks called **cache lines** (typically 64 bytes). When two independent threads running on separate CPU cores read and write to distinct `volatile` variables that reside on the same cache line:
1. Core 1 updates `x`, invalidating the cache line in Core 2's L1/L2 cache via the **MESI cache coherence protocol**.
2. Core 2 updates `y`, which in turn invalidates the cache line in Core 1's cache.
3. This triggers a continuous **cache line invalidation storm (cache ping-pong)** over the CPU interconnect, stalling pipeline execution even though the threads are modifying logically independent variables.

### The Mitigation: Cache-Line Padding
By injecting 64 bytes of padding (8 × 8-byte `long` primitive fields `p1` to `p8`) between the two volatile variables `x` and `y`, each variable is guaranteed to reside on a distinct cache line.

```java
// False-Sharing/PaddedCounter.java
static class Counters {
    volatile long x = 0;

    // 64 bytes of cache line padding (8 * 8 bytes = 64 bytes)
    long p1, p2, p3, p4;
    long p5, p6, p7, p8;

    volatile long y = 0;
}
```

### Benchmark Results
Both benchmarks execute **100,000,000 increments** per thread across 2 concurrent threads on a multi-core machine running Java 21 LTS:

| Implementation | Description | Execution Time | Speedup | Cache Behavior |
| :--- | :--- | :---: | :---: | :--- |
| `UnpaddedCounter` | `x` and `y` share a 64-byte line | **~1,710 ms** | 1.0x (baseline) | Severe MESI bus invalidation ping-pong |
| `PaddedCounter` | `x` and `y` isolated by 64B padding | **~146 ms** | **~11.7x faster** | Independent cache lines, zero false sharing |

> **Performance Impact:** Eliminating false sharing yielded an **11.7x throughput increase (~91.5% latency reduction)** without altering algorithmic logic.

#### Reproducing the Benchmark
```powershell
# Compile the benchmark classes
javac False-Sharing/UnpaddedCounter.java False-Sharing/PaddedCounter.java

# Run unpadded benchmark (severe false sharing)
java -cp False-Sharing UnpaddedCounter

# Run padded benchmark (false sharing eliminated)
java -cp False-Sharing PaddedCounter
```

---

## 2. Lock-Free CAS Ring Buffer (`Lock-Free-Cas/`)

### Architecture Overview
Traditional thread synchronization uses blocking mutexes (e.g., `synchronized` or `ReentrantLock`), which introduce kernel-level context switching, priority inversion, and lock contention. 

The `inventory_ringbuffer` module implements a high-throughput, non-blocking **circular ring buffer** utilizing hardware-level Compare-And-Swap (CAS) instructions (`AtomicLong.compareAndSet` → x86 `LOCK CMPXCHG`):

- **Bounded Atomic Storage**: Backed by `AtomicReferenceArray<Object>` for lock-free slot indexing.
- **Atomic Head & Tail Sequences**: Tracks producer and consumer positions with `AtomicLong`.
- **Single-Consumer Multi-Producer (SCMP)**: Multiple producer threads concurrently enqueue purchase requests using lock-free CAS loops without acquiring locks.
- **Yield-based Backoff**: Threads yield execution when full or empty conditions occur without blocking the OS thread.

```text
  Producers (Threads 1..N)           Ring Buffer (CAS Slots)               Consumer Thread
 [Producer 1] ──┐                 ┌───┬───┬───┬───┬───┬───┐
 [Producer 2] ──┼──► buffer.offer()│ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │──► buffer.poll() ──► Inventory.reserve()
 [Producer N] ──┘   (tail.CAS)    └───┴───┴───┴───┴───┴───┘    (head.CAS)       (Zero Contention)
```

### Concurrency Stress Test Harness
The `RingBufferInventoryDemo` exercises the ring buffer under high concurrency stress:
- **Producers**: Multiple parallel threads generating purchase requests.
- **Consumer**: Dedicated consumer thread draining the buffer and deducting inventory.
- **Validation**: Strict invariant validation (`finalStock == expectedStock && finalStock >= 0 && success + failed == totalAttempts`).

#### Reproducing the Test
```powershell
# Compile with destination directory matching the package structure
javac -d . Lock-Free-Cas/inventory_ringbuffer/*.java

# Run the interactive stress test
java com.example.inventory.ringbuffer.RingBufferInventoryDemo
```

#### Sample Test Output
```text
=== Ring Buffer Inventory Demo ===

Initial stock: 1000
Threads: 4
Purchases per thread: 500
Total purchase attempts: 2000

Successful purchases: 1000
Failed purchases: 1000

Final stock: 0
Expected stock: 0

Correct: true
```

---

## 3. Race Conditions & Data Races (`Race-Condition/`)

This module demonstrates the distinction between low-level **Data Races** (concurrent unsynchronized read/write to memory) and high-level **Race Conditions** (flawed operation ordering / TOCTOU).

### A. Data Race: IRCTC Ticket Booking (`Race-Condition/Data-Race/`)
- **Vulnerability (`IrctcExample.java`)**: Two threads check `seat > 0` and decrement `seat--` without memory visibility or mutual exclusion guarantees. Both threads pass the check and book the single remaining seat, corrupting state.
- **Fix (`IrctcExampleFixed.java`)**: Introduces intrinsic monitor synchronization (`synchronized static void BookSeat(...)`), enforcing mutual exclusion so only one thread books the seat while the other fails gracefully.

```powershell
# Compile and run data race demonstration
javac Race-Condition/Data-Race/IrctcExample.java
java -cp Race-Condition/Data-Race IrctcExample

# Compile and run synchronized fix
javac Race-Condition/Data-Race/IrctcExampleFixed.java
java -cp Race-Condition/Data-Race IrctcExampleFixed
```

### B. TOCTOU Inventory Race: Pizza Shop (`Race-Condition/Race-condition/`)
- **Vulnerability (`Main.java`)**: Simulates a classic **Time-Of-Check to Time-Of-Use (TOCTOU)** window. Two customer threads check `pizzaStock > 0`, sleep for 100 ms (simulating network or I/O latency), and then decrement `pizzaStock--`. Both customers order when only 1 pizza is available.

```powershell
javac Race-Condition/Race-condition/Main.java
java -cp Race-Condition/Race-condition Main
```

### C. Check-Then-Act Race: Username Registration (`Race-Condition/UsernameValidation-Race/`)
- **Vulnerability (`UsernameRaceInTeamUnix.java`)**: Two concurrent threads (`Dev` and `Mahak`) attempt to register the identical username (`"dev47929"`). 
- Because checking membership (`!registeredUsers.contains(username)`) and inserting (`registeredUsers.add(username)`) is not atomic on standard collections like `HashSet`, both threads pass the check simultaneously.
- Furthermore, `HashSet` is not thread-safe, leading to race-condition anomalies and potential internal bucket corruption.

```powershell
javac Race-Condition/UsernameValidation-Race/UsernameRaceInTeamUnix.java
java -cp Race-Condition/UsernameValidation-Race UsernameRaceInTeamUnix
```

---

## Benchmark & Test Summary Matrix

| Module | Experiment / Test | Primary Concurrency Mechanism | Key Metric / Result |
| :--- | :--- | :--- | :--- |
| **False-Sharing** | Unpadded vs. Padded Counter | Cache line padding (64 bytes) | **11.7x throughput speedup** (~1,710 ms vs ~146 ms) |
| **Lock-Free-CAS** | Ring Buffer Inventory | `AtomicLong.compareAndSet`, `AtomicReferenceArray` | Lock-free high-throughput queue, zero overselling |
| **Race-Condition** | IRCTC Ticket Booking | Unsynchronized vs. `synchronized` | Demonstrates double booking vs. mutual exclusion |
| **Race-Condition** | Pizza Shop Stock | Simulated I/O latency window | Stock overdraw / TOCTOU vulnerability |
| **Race-Condition** | Username Registration | Non-atomic check-then-act (`HashSet`) | Concurrent duplicate account registration |

---

## Technical Takeaways for Systems Engineers

1. **Memory Layout Matters**: Algorithmic complexity ($O(1)$ vs $O(N)$) does not tell the whole story. Spatial locality and cache line boundaries (64 bytes) can degrade execution by an order of magnitude due to hardware cache coherence invalidations.
2. **Lock Contention vs. Atomics**: Mutual exclusion primitives incur operating system scheduling and context-switch penalties. CAS-based lock-free primitives allow non-blocking concurrency without sleep/wake transitions.
3. **Compound Operations Require Atomicity**: Concurrency bugs rarely come from single variable reads; they manifest in check-then-act patterns (`if (valid) { use(); }`) where the state changes between check and execution.

---

## How to Run All Tests

To execute the entire test and benchmark suite locally:

```powershell
# 1. False-Sharing Benchmark
javac False-Sharing/UnpaddedCounter.java False-Sharing/PaddedCounter.java
Write-Host "--- Running Unpadded Counter ---"
java -cp False-Sharing UnpaddedCounter
Write-Host "--- Running Padded Counter ---"
java -cp False-Sharing PaddedCounter

# 2. Lock-Free Ring Buffer Demo
javac -d . Lock-Free-Cas/inventory_ringbuffer/*.java
"1000`n4`n500`n64`n" | java com.example.inventory.ringbuffer.RingBufferInventoryDemo

# 3. Race Condition Demos
javac Race-Condition/Data-Race/IrctcExample.java Race-Condition/Data-Race/IrctcExampleFixed.java
java -cp Race-Condition/Data-Race IrctcExample
java -cp Race-Condition/Data-Race IrctcExampleFixed

javac Race-Condition/Race-condition/Main.java
java -cp Race-Condition/Race-condition Main

javac Race-Condition/UsernameValidation-Race/UsernameRaceInTeamUnix.java
java -cp Race-Condition/UsernameValidation-Race UsernameRaceInTeamUnix
```
