import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class PowderClient {
    private final String SCHEME = "http://";
    private String serverPath = SCHEME + "localhost:8080/powder";
    private int maxThreads = 256;
    private int numSkiers = 50000;
    private int numLifts = 40;
    private int skiDay = 1;
    private String resortID = "SilverMt";

    public PowderClient() {
        setParameters();
    }

    public static void main(String[] args) throws InterruptedException {
        // initialize client and set server path
        PowderClient client = new PowderClient();

        int numThreadsPhase1 = client.maxThreads / 4;
        int numThreadsPhase2 = client.maxThreads;
        int numThreadsPhase3 = client.maxThreads / 4;
        CountDownLatch phase1End = new CountDownLatch(numThreadsPhase1);
        CountDownLatch phase2Start = new CountDownLatch((numThreadsPhase1 + 10 - 1) / 10);
        CountDownLatch phase2End = new CountDownLatch(numThreadsPhase2);
        CountDownLatch phase3Start = new CountDownLatch((numThreadsPhase2 + 10 - 1) / 10);
        CountDownLatch phase3End = new CountDownLatch(numThreadsPhase3);
        List<CountDownLatch> phase1Latches = Arrays.asList(phase1End, phase2Start);
        List<CountDownLatch> phase2Latches = Arrays.asList(phase2End, phase3Start);
        List<CountDownLatch> phase3Latches = Arrays.asList(phase3End);

        LoadGenerator phase1 = new LoadGenerator(client.serverPath, phase1Latches, numThreadsPhase1,
                                    0, client.numSkiers, 1, 91);
        LoadGenerator phase2 = new LoadGenerator(client.serverPath, phase2Latches, numThreadsPhase2,
                0, client.numSkiers, 91, 361);
        LoadGenerator phase3 = new LoadGenerator(client.serverPath, phase3Latches, numThreadsPhase3,
                0, client.numSkiers, 361, 421, true);

        System.out.println("p1");
        phase1.run();
        new Thread(phase1).start();
        phase2Start.await();
        System.out.println("p2");
        new Thread(phase2).start();
        phase3Start.await();
        System.out.println("p3");
        new Thread(phase3).start();
        phase1End.await();
        phase2End.await();
        phase3End.await();

        System.out.println("all phases finished");

    }

    // set parameters of the client from console inputs
    private void setParameters() {
        Scanner inputScan = new Scanner(System.in);

        System.out.println("Please enter the maximum number of threads to run (Default 256): ");
        try {
            int inputMaxThreads = Integer.parseInt(inputScan.nextLine());
            if (inputMaxThreads <= 0) throw new NumberFormatException();
            this.maxThreads = inputMaxThreads;
        } catch (NumberFormatException e) {
            System.out.println("default value used");
        }
        System.out.println("maxThreads is " + this.maxThreads + "\n");

        System.out.println("Please enter the number of skiers (Default 50,000):");
        try {
            int inputNumSkiers = Integer.parseInt(inputScan.nextLine());
            if (inputNumSkiers <= 0) throw new NumberFormatException();
            this.numSkiers = inputNumSkiers;
        } catch (NumberFormatException e) {
            System.out.println("default value used");
        }
        System.out.println("numSkiers is " + this.numSkiers + "\n");

        System.out.println("Please enter the number of ski lifts (Range 5 - 60, default 40):");
        try {
            int inputNumLifts = Integer.parseInt(inputScan.nextLine());
            if (inputNumLifts < 5 || inputNumLifts > 60) throw new NumberFormatException();
            this.numLifts = inputNumLifts;
        } catch (NumberFormatException e) {
            System.out.println("default value used");
        }
        System.out.println("numLifts is " + this.numLifts + "\n");

        System.out.println("Please enter the ski day (Default 1):");
        try {
            int inputSkiDay = Integer.parseInt(inputScan.nextLine());
            if (inputSkiDay <= 0) throw new NumberFormatException();
            this.skiDay = inputSkiDay;
        } catch (NumberFormatException e) {
            System.out.println("default value used");
        }
        System.out.println("skiDay is " + this.skiDay + "\n");

        System.out.println("Please enter the the resort name (Default SilverMt):");
        String inputResortId = inputScan.nextLine();
        if (inputResortId.isEmpty()) {
            System.out.println("default value used");
        } else {
            this.resortID = inputResortId;
        }
        System.out.println("resortId is " + this.resortID + "\n");

        System.out.println("Please enter address of the server including port (Default localhost):");
        String inputServerPath = inputScan.nextLine();
        if (inputServerPath.isEmpty()) {
            System.out.println("default value used");
        } else {
            this.serverPath = this.SCHEME + inputServerPath;
        }
        System.out.println("serverPath is " + this.serverPath + "\n");
    }
}
