#include <iostream>
#include <atomic>
#include <thread>
using namespace std;

class Inventory {
    atomic<int> stock;

public:
    Inventory(int s) {
        stock = s;
    }

    bool buy() {
        int cur = stock.load();
        while (cur > 0) {
            if (stock.compare_exchange_weak(cur, cur - 1))
                return true;
        }
        return false;
    }
    int left() { return stock.load(); }
};

Inventory inv(5000000);
atomic<int> bought(0);

void worker() {
    for (int j = 0; j < 1000000; j++) {
        if (inv.buy()) bought++;
    }
}

int main()
{
    thread t[8];

    for (int i = 0; i < 8; i++) {
        t[i] = thread(worker);
    }

    for (int i = 0; i < 8; i++) {
        t[i].join();
    }


    cout << "attempts  : " << 8000000 << endl;
    cout << "bought    : " << bought << endl;
    cout << "stock left: " << inv.left() << endl;
    return 0;
}