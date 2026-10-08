
class PizzaShop {
    int pizzaStock = 1;

    void orderPizza(String customer) {
        if (pizzaStock > 0) {
            System.out.println(customer + " checked stock: " + pizzaStock);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            pizzaStock--;
            System.out.println(customer + " ordered a pizza!");
        } 
        else {
            System.out.println(customer + ": No pizza available!");
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        PizzaShop shop = new PizzaShop();

        Thread t1 = new Thread(() -> shop.orderPizza("Customer A"));
        Thread t2 = new Thread(() -> shop.orderPizza("Customer B"));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Final pizza stock: " + shop.pizzaStock);
    }
}
