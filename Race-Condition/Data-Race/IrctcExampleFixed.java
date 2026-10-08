/*
we executed 

expected output : (NOTE: any user can book seat firstly it depends which thread reach first and execute)
USer2checkout seat and found succesfully
USer2Seat booked Succesfully
NO seat available 

OR
User1checkout seat and found succesfully
User1Seat booked Succesfully
NO seat available

*/

public class IrctcExampleFixed {
    static int seat=1;
 
     // synchronized lock the current thread till one completes the task it prevent multiple threads working simultanously at a time
    static synchronized void BookSeat(String user){
     if(seat>0){
        System.out.println(user + "checkout seat and found succesfully");

        try{
            Thread.sleep(100);
        }catch(InterruptedException e){
                  Thread.currentThread().interrupt();
        }
          seat--;
      System.out.println(user + "Seat booked Succesfully");
    
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
