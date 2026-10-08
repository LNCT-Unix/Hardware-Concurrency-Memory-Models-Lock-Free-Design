 Prepare a benchmark and testing file for this in one page simple and concise with one table#include <iostream>
#include <thread>
#include <atomic>

using namespace std;

int order = 0;
atomic<bool> ready(false);

// Producer: prepares the order
void prepareOrder() {
    order = 100;  // Order is prepared

    // Tell the other thread that the order is ready
    ready.store(true, memory_order_release);
}

// Consumer: delivers the order
void deliverOrder() {
    // Wait until the order is ready
    while (!ready.load(memory_order_acquire)) {
    }

    cout << "Order " << order << " is ready for delivery!" << endl;
}

int main() {
    thread producer(prepareOrder);
    thread consumer(deliverOrder);

    producer.join();
    consumer.join();

    return 0;
}