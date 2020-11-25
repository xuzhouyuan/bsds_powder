import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PowderClient {
    private static String[] ApiNames = new String[]{
            "writeNewLiftRide",
            "getSkierDayVertical",
            "getSkierResortTotals"
    };

    private final String SCHEME = "http://";
    private String serverPath = SCHEME + "54.213.238.147:8080/powder";
    private int maxThreads = 256;
    private int numSkiers = 20000;
    private int numLifts = 40;
    private int skiDay = 1;
    private String resortID = "SilverMt";

    private ExecutorService executor = Executors.newCachedThreadPool();

    private static Logger logger = LogManager.getLogger(PowderClient.class);

    public PowderClient() {
        setParameters();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        // clear database
        BasicDataSource dataSource = DBCPDataSource.getDataSource();
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "TRUNCATE powder_run.LiftRides";
        try {
            conn = dataSource.getConnection();
            preparedStatement = conn.prepareStatement(insertQueryStatement);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }

        // initialize client
        PowderClient client = new PowderClient();
        Map<String, BlockingQueue> latencyQueues = new HashMap<>();
        for (String ApiName: ApiNames) {
            latencyQueues.put(ApiName, new LinkedBlockingDeque());
        }
        client.runPhases(latencyQueues);
        client.processLatencyStat(latencyQueues);
        System.out.println("Done");
    }

    /**
     * @return The total number of requests made
     */
    public int runPhases(Map<String, BlockingQueue> latencyQueues) throws InterruptedException, IOException {
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

        LoadGenerator phase1 = new LoadGenerator(executor, serverPath, phase1Latches, requestCountPhase1,
                latencyQueues, numThreadsPhase1, resortID, skiDay, numLifts,
                0, numSkiers, 1, 91);
        LoadGenerator phase2 = new LoadGenerator(executor, serverPath, phase2Latches, requestCountPhase2,
                latencyQueues, numThreadsPhase2, resortID, skiDay, numLifts,
                0, numSkiers, 91, 361);
        LoadGenerator phase3 = new LoadGenerator(executor, serverPath, phase3Latches, requestCountPhase3,
                latencyQueues, numThreadsPhase3, resortID, skiDay, numLifts,
                0, numSkiers, 361, 421, true);

        logger.info(this.getClass().getName() + " starts to run phases.");
        logger.info(this.getClass().getName() + " serverPath is " + serverPath);
        System.out.println("phase 1 starting");
        long startNano = System.nanoTime();
        executor.submit(phase1);
        phase2Start.await();
        System.out.println("phase 2 starting");
        executor.submit(phase2);
        phase3Start.await();
        System.out.println("phase 3 starting");
        executor.submit(phase3);
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
        return totalRequest;
    }

    private void processLatencyStat(Map<String, BlockingQueue> latencyQueues) throws IOException {
        // TODO: [in the future] create thread to write latencyQueue to csv file,
        //  process stats from file after completion.


        //TODO: modify to process per api
        for (String ApiName: ApiNames) {
            BlockingQueue<String> latencyQueue = latencyQueues.get(ApiName);
            List<Integer> latency = new ArrayList<>();
            File csvOutput = File.createTempFile("powder-client-latency-" + ApiName + "-",
                    ".csv");
            System.out.println(ApiName + " latency entries stored at " + csvOutput.getAbsolutePath());
            FileWriter writer = new FileWriter(csvOutput.getAbsolutePath());
            while (!latencyQueue.isEmpty()) {
                String entryString = latencyQueue.poll();
                String[] entry = entryString.split(",");
                // writing to csv
                writer.write(entryString+"\n");
                latency.add(Integer.parseInt(entry[1]));
            }
            writer.close();
            Collections.sort(latency);

            System.out.println("Latency (ms) statistics for " + ApiName + ": ");
            System.out.print("  MEAN  : ");
            System.out.println(findMean(latency));
            System.out.print("  MEDIAN: ");
            System.out.println(findMedian(latency));
            System.out.print("  P99   : ");
            System.out.println(findP99(latency));
            System.out.print("  MAX   : ");
            System.out.println(latency.get(latency.size()-1));
        }
        return;
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
        String inputResortID = inputScan.nextLine();
        if (inputResortID.isEmpty()) {
            System.out.println("default value used");
        } else {
            this.resortID = inputResortID;
        }
        System.out.println("resortID is " + this.resortID + "\n");

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
