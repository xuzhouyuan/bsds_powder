import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PowderClient {
    private final String SCHEME = "http://";
    private String serverPath = SCHEME + "34.223.113.123:8080/powder/";
    private int maxThreads = 256;
    private int numSkiers = 20000;
    private int numLifts = 40;
    private int skiDay = 1;
    private String resortID = "SilverMt";

    private static Logger logger = LogManager.getLogger(PowderClient.class);

    public PowderClient() {
        setParameters();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        // initialize client
        PowderClient client = new PowderClient();
        BlockingQueue<String> latencyQueue = new LinkedBlockingQueue<>();
        client.runPhases(latencyQueue);
    }

    /**
     * @return The total number of requests made
     */
    public int runPhases(BlockingQueue<String> latencyQueue) throws InterruptedException, IOException {
        int numThreadsPhase1 = maxThreads / 4;
        int numThreadsPhase2 = maxThreads;
        int numThreadsPhase3 = maxThreads / 4;
        CountDownLatch phase1End = new CountDownLatch(numThreadsPhase1);
        CountDownLatch phase2Start = new CountDownLatch((numThreadsPhase1 + 10 - 1) / 10);
        CountDownLatch phase2End = new CountDownLatch(numThreadsPhase2);
        CountDownLatch phase3Start = new CountDownLatch((numThreadsPhase2 + 10 - 1) / 10);
        CountDownLatch phase3End = new CountDownLatch(numThreadsPhase3);
        List<CountDownLatch> phase1Latches = Arrays.asList(phase1End, phase2Start);
        List<CountDownLatch> phase2Latches = Arrays.asList(phase2End, phase3Start);
        List<CountDownLatch> phase3Latches = Arrays.asList(phase3End);
        RequestCount requestCountPhase1 = new RequestCount();
        RequestCount requestCountPhase2 = new RequestCount();
        RequestCount requestCountPhase3 = new RequestCount();

        LoadGenerator phase1 = new LoadGenerator(serverPath, phase1Latches, requestCountPhase1,
                latencyQueue, numThreadsPhase1, resortID, skiDay, numLifts,
                0, numSkiers, 1, 91);
        LoadGenerator phase2 = new LoadGenerator(serverPath, phase2Latches, requestCountPhase2,
                latencyQueue, numThreadsPhase2, resortID, skiDay, numLifts,
                0, numSkiers, 91, 361);
        LoadGenerator phase3 = new LoadGenerator(serverPath, phase3Latches, requestCountPhase3,
                latencyQueue, numThreadsPhase3, resortID, skiDay, numLifts,
                0, numSkiers, 361, 421, true);

        logger.info(this.getClass().getName() + " starts to run phases.");
        logger.info(this.getClass().getName() + " serverPath is " + serverPath);
        System.out.println("phase 1 starting");
        long startNano = System.nanoTime();
        new Thread(phase1).start();
        phase2Start.await();
        System.out.println("phase 2 starting");
        new Thread(phase2).start();
        phase3Start.await();
        System.out.println("phase 3 starting");
        new Thread(phase3).start();
        phase1End.await();
        phase2End.await();
        phase3End.await();
        long endNano = System.nanoTime();
        phase1.sumRequestCounter();
        phase2.sumRequestCounter();
        phase3.sumRequestCounter();

        System.out.println("All phases completed");
        int success = requestCountPhase1.success + requestCountPhase2.success + requestCountPhase3.success;
        int failure = requestCountPhase1.failure + requestCountPhase2.failure + requestCountPhase3.failure;
        int totalRequest = success + failure;
        System.out.println("Total number successful requests: " + success);
        System.out.println("Total number of failed requests: " + failure);
        int wallTime = (int) ((endNano - startNano) / 1000000); // millisecond
        System.out.println("Total run time (wall time): " + wallTime + " ms");
        float throughput = ((float)totalRequest / wallTime) * 1000;
        System.out.println("Throughput is: " + throughput + " req/sec");
        processLatencyStat(latencyQueue);
        return totalRequest;
    }

    private String[] processLatencyStat(BlockingQueue<String> latencyQueue) throws IOException {
        // TODO: [in the future] create thread to write latencyQueue to csv file,
        //  process stats from file after completion.
        List<Integer> latencyPOST = new ArrayList<>();
        List<Integer> latencyGET = new ArrayList<>();
        File csvOutputPOST = File.createTempFile("powder-client-request-latency-POST-", ".csv");
        File csvOutputGET = File.createTempFile("powder-client-request-latency-GET-", ".csv");
        System.out.println("POST latency entries stored at " + csvOutputPOST.getAbsolutePath());
        System.out.println("GET latency entries stored at " + csvOutputGET.getAbsolutePath());
        FileWriter writerPOST = new FileWriter(csvOutputPOST.getAbsolutePath());
        FileWriter writerGET = new FileWriter(csvOutputGET.getAbsolutePath());
        while (!latencyQueue.isEmpty()) {
            String entryString = latencyQueue.poll();
            String[] entry = entryString.split(",");
            if (entry[1].equals("POST")) {
                writerPOST.write(entryString+"\n");
                latencyPOST.add(Integer.parseInt(entry[2]));
            } else if (entry[1].equals("GET")) {
                writerGET.write(entryString+"\n");
                latencyGET.add(Integer.parseInt(entry[2]));
            }
        }
        writerPOST.close();
        writerGET.close();
        Collections.sort(latencyPOST);
        Collections.sort(latencyGET);

        System.out.println("Latency (ms) statistics for POST: ");
        System.out.print("  MEAN  : ");
        System.out.println(findMean(latencyPOST));
        System.out.print("  MEDIAN: ");
        System.out.println(findMedian(latencyPOST));
        System.out.print("  P99   : ");
        System.out.println(findP99(latencyPOST));
        System.out.print("  MAX   : ");
        System.out.println(latencyPOST.get(latencyPOST.size()-1));

        System.out.println("Latency (ms) statistics for GET: ");
        System.out.print("  MEAN  : ");
        System.out.println(findMean(latencyGET));
        System.out.print("  MEDIAN: ");
        System.out.println(findMedian(latencyGET));
        System.out.print("  P99   : ");
        System.out.println(findP99(latencyGET));
        System.out.print("  MAX   : ");
        System.out.println(latencyGET.get(latencyGET.size()-1));
        return new String[] {csvOutputPOST.getAbsolutePath(), csvOutputGET.getAbsolutePath()};
    }

    private int findMedian(List<Integer> list) {
        int length = list.size();
        if (length % 2 == 0) {
            return (int) Math.round((list.get(length/2) + list.get((length/2)+1)) / 2.0);
        }
        return list.get(length/2);
    }

    private int findMean(List<Integer> list) {
        return (int) Math.round(list.stream().mapToInt(i -> i).average().orElse(-1.0));
    }

    private Integer findP99(List<Integer> list) {
        if (list.size() == 0) {
            return -1;
        }
        return list.get(list.size() * 99 / 100);
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
