# Username Validation Race Condition: Benchmark & Testing

## 1. Overview
In [`UsernameRaceInTeamUnix.java`](file:///e:/1.Code/ResumeProject1/Hardware-Concurrency-Memory-Models-Lock-Free-Design/Race-Condition/UsernameValidation-Race/UsernameRaceInTeamUnix.java), concurrent user registration exhibits a classic **Check-Then-Act (TOCTOU)** race condition:

```java
// Vulnerability: Non-atomic check and insert
if (!registeredUsers.contains(username)) {
    registeredUsers.add(username); // Interleaved thread can execute between contains() and add()
}
```
Two threads can evaluate `!registeredUsers.contains(username)` as `true` simultaneously, allowing both to register the identical username (`"dev47929"`), corrupting data invariants.

---

## 2. Benchmark & Concurrency Test Results

Testing was conducted using [`UsernameRaceBenchmark.java`](file:///e:/1.Code/ResumeProject1/Hardware-Concurrency-Memory-Models-Lock-Free-Design/Race-Condition/UsernameValidation-Race/UsernameRaceBenchmark.java) under two rigorous workloads:
1. **Invariant Race Test**: 10,000 concurrent collision attempts for the identical username.
2. **Throughput Benchmark**: 1,000,000 operations across 4 concurrent worker threads on Java 21 LTS.

| Implementation Strategy | Concurrency Primitive | Invariant Guarantee | Duplicate Collisions (10k Runs) | Execution Time (1M Ops) | Throughput / Scalability |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Unsynchronized `HashSet`** | None (Raw mutable state) | **BROKEN** | **~52 duplicates** (~0.52% failure) | N/A (Data corruption / CME) | Unsafe for multi-threading |
| **Synchronized `HashSet`** | Intrinsic Mutex (`synchronized`) | **PRESERVED** | **0 duplicates** (0.00% failure) | **~117 ms** | Baseline thread-safe (Lock contention) |
| **`ConcurrentHashMap.newKeySet()`** | Lock-Free CAS / Striped Buckets | **PRESERVED** | **0 duplicates** (0.00% failure) | **~41 ms** | **~2.85x faster** (High scalability) |

---

## 3. How to Run & Verify

### Run the Invariant & Throughput Benchmark:
```powershell
javac Race-Condition/UsernameValidation-Race/UsernameRaceBenchmark.java
java -cp Race-Condition/UsernameValidation-Race UsernameRaceBenchmark
```

### Run the Original Race Condition Demonstration:
```powershell
javac Race-Condition/UsernameValidation-Race/UsernameRaceInTeamUnix.java
java -cp Race-Condition/UsernameValidation-Race UsernameRaceInTeamUnix
```
