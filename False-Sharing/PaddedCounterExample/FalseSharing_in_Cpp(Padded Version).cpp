
#include <chrono>
#include <functional>  
#include <iostream>
#include <thread>
#include <vector>
using namespace std;

const int NUM_THREADS = 8;
const int IMAGE_SIZE = 80000000;
const int PAD         = 8;   

alignas(64) double partial[NUM_THREADS * PAD];   

void work(const vector<int>& image, int t) {
    int chunk = IMAGE_SIZE / NUM_THREADS;
    volatile double* out = &partial[t * PAD];    
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
    for (int t = 0; t < NUM_THREADS; t++) total += partial[t * PAD];

    cout << "Padded:   " << ms << " ms (total = " << total << ")\n";
}
