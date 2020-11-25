import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueConsumer {
    private static final int NUM_WORKER = 64;

    public static void main(String[] args) {

        LiftRideDao dao = new LiftRideDao();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKER);


        for (int i = 0; i < NUM_WORKER; i++) {
            executor.submit(new ConsumerWorker(dao));
        }

    }


}