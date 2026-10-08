// we execute and book the tickit in synchronized way until one thread/user finished booking
//prevent two theads to work simultaneously . we lock current thread and unlock after task finished 

public class DataRaceDemoFixed1 {
       static int stock=1;
    static synchronized void buy(String customer){      
        if (stock>0){

            System.out.println(customer +" found the product");
            try{
                Thread.sleep(100);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            stock--;
            System.out.println(customer+" bought the product");
            
        }else
            {
            System.out.println("Out of stock");
        }
    }
    public static void main(String[] args) {
        Thread customer1=new Thread(()-> buy("Customer1"));
        Thread customer2=new Thread(()->buy("Customer2"));

        customer1.start();
        customer2.start();
    }
}
