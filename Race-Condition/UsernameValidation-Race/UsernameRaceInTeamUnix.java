import java.util.HashSet;
import java.util.Set;

public class UsernameRaceInTeamUnix {

    static Set<String> registeredUsers = new HashSet<>();

    static void register(String username, String requester) {
        if (!registeredUsers.contains(username)) {
            //If username is free, claim it. Another thread may have done the same check in between.
            registeredUsers.add(username);
            System.out.println(requester + " registered '" + username + "'");
        } else {
            System.out.println(requester + " rejected: '" + username + "' is taken");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        registeredUsers = new HashSet<>();
        Thread mahak = new Thread(() -> register("dev47929", "Dev"));
        Thread dev   = new Thread(() -> register("dev47929", "Mahak"));

        dev.start();
        mahak.start();

        dev.join();
        mahak.join();

        // Both may have registered, so the same username can be claimed twice.
        System.out.println("Registered users: " + registeredUsers);
    }
}