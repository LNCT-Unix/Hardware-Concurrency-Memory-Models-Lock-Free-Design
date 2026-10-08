// Product stock probblem when 1 product is available and multiple user tries to book it 

public class StockProbblem{

    static int stock=1;
    static  void buy(String customer){      //synchronised to fix the probblem
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