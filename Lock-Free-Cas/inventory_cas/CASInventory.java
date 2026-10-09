import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class CASInventory{
    private static AtomicInteger stock;
    public static boolean purchase(String customer){
        while(true){
            int currentStock = stock.get();
            if(currentStock<=0){
                return false;
            }
            int newStock=currentStock-1;
            if(stock.compareAndSet(currentStock,newStock)){
                return true;
            }
        }
    }
public static void main(String[] args) throws Exception{
    Scanner sc = new Scanner(System.in);
      System.out.print("Enter initial stock: ");
        int initialStock = sc.nextInt();

        System.out.print("Enter number of customers: ");
        int numberOfCustomers = sc.nextInt();

       
        stock = new AtomicInteger(initialStock);

        Thread[] customers = new Thread[numberOfCustomers];

       
        for (int i = 0; i < numberOfCustomers; i++) {

            final int customerId = i + 1;

            customers[i] = new Thread(() -> {

                boolean success = purchase("Customer-" + customerId);
                if (success) {
                    System.out.println(
                        "Customer-" + customerId +
                        " → Purchase successful"
                    );
                } else {
                    System.out.println(
                        "Customer-" + customerId +
                        " → OUT OF STOCK"
                    );
                }
            });
        }

        long startTime = System.nanoTime();
        for (Thread customer : customers) {
            customer.start();
        }

        
        for (Thread customer : customers) {
            customer.join();
        }
        long endTime = System.nanoTime();

        long executionTimeNs = endTime - startTime;

        System.out.println("\n------------------------");
        System.out.println("Initial Stock : " + initialStock);
        System.out.println("Customers     : " + numberOfCustomers);
        System.out.println("Final Stock   : " + stock.get());
        System.out.println("Execution Time: " + executionTimeNs + " ns");
        System.out.println("------------------------");
        sc.close();
}

}