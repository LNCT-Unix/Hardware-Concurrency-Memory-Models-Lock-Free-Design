 /* Idea    : 8 threads sum parts of an image, but their results sit
            side by side in one cache line, so threads slow each other down.
  Compare : Run the padded version and compare the time. */

#include <chrono>
#include <functional>   
#include <iostream>
#include <thread>
#include <vector>
using namespace std;

const int NUM_THREADS = 8;
const int IMAGE_SIZE  = 80000000;  

double partial[NUM_THREADS];   

void work(const vector<int>& image, int t) {
    int chunk = IMAGE_SIZE / NUM_THREADS;
    volatile double* out = &partial[t];      
    for (int i = t * chunk; i < (t + 1) * chunk; i++)
        *out = *out + image[i];             
}

int main() {
    vector<int> image(IMAGE_SIZE, 1);
    vector<thread> threads;

    auto start = chrono::steady_clock::now();

    for (int t = 0; t < NUM_THREADS; t++)
        threads.emplace_back(work, cref(image), t);
    for (auto& th : threads) th.join();

    auto end = chrono::steady_clock::now();
    double ms = chrono::duration<double, milli>(end - start).count();

    double total = 0;
    for (int t = 0; t < NUM_THREADS; t++) total += partial[t];

    cout << "Unpadded: " << ms << " ms (total = " << total << ")\n";
}
