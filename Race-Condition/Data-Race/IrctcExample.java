//Ashutosh Prajapat
/* 
IRCTC / any tickit booking system
when Multiple users try to book seats at a same time . the Data Race condition happens both user can book same seat 

expected output :
User1 checkout seat and found succesfully
USer2 checkout seat and found succesfully
USer2 Seat booked Succesfully
User1 Seat booked Succesfully

NOTE-> Any thread can perform first eg-> user 2 may found seat first

 */

public class IrctcExample {

    static int seat=1;

    static void BookSeat(String user){
     if(seat>0){
        System.out.println(user + " checkout seat and found succesfully");

        try{
            Thread.sleep(100);
        }catch(InterruptedException e){
                  Thread.currentThread().interrupt();
        }
          seat--;
      System.out.println(user + " Seat booked Succesfully");
    
     }else{
    System.out.println("NO seat available");
     }
    }
    public static void main(String[] args) {
        Thread user1=new Thread(()->BookSeat("User1"));
        Thread user2=new Thread(()->BookSeat("USer2"));
        user1.start();
        user2.start();
    }
}
