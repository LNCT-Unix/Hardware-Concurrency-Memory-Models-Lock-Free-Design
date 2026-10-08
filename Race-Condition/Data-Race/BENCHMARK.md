# Data Race Benchmark Report

## Goal

Check what happens when two users try to book one seat at the same time, and compare the normal version with the synchronized version.

## Setup

- Java programs: `IrctcExample` and `IrctcExampleFixed`
- Starting seats: 1
- Users: 2 threads
- Each program was run once.

## Result

| Version | Seats reported as booked | Result |
| --- | ---: | --- |
| Without synchronization | 2 | Both users booked the same seat |
| With `synchronized` | 1 | One user booked the seat; the other saw no seat available |

The order of the users can change between runs. This was a basic behavior check, not a timing benchmark, so no speed comparison was made.

## Conclusion

The unsynchronized version can overbook because both threads may see the seat as available before either updates it. Synchronizing the booking method makes the check and update happen one at a time, which avoids this result.
