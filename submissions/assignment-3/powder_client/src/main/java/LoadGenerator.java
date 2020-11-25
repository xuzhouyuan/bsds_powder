import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class LoadGenerator implements Runnable{
    private String serverPath;
    private List<CountDownLatch> latches;
    private String resortID;
    private String dayID;
    private int numThreads;
    private int numLifts;
    private int startSkierID, endSkierID;
    private int startMinute, endMinute;
    private boolean coolDown = false;

    private ExecutorService executor;

    private List<RequestCount> requestCounter;
    private RequestCount requestCount;
    private Map<String, BlockingQueue> latencyQueues;
    private static Logger logger = LogManager.getLogger(PowderClient.class);

    public LoadGenerator(ExecutorService executor, String serverPath, List<CountDownLatch> latches, RequestCount requestCount,
                         Map<String, BlockingQueue> latencyQueues, int numThreads,String resortID, int dayID, int numLifts,
                         int startSkierID, int endSkierID, int startMinute, int endMinute) {
        this.executor = executor;
        this.serverPath = serverPath;
        this.latches = latches;
        this.requestCount = requestCount;
        this.latencyQueues = latencyQueues;
        this.numThreads = numThreads;
        this.resortID = resortID;
        this.dayID = Integer.toString(dayID);
        this.numLifts = numLifts;
        this.startSkierID = startSkierID;
        this.endSkierID = endSkierID;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        requestCounter = new ArrayList<>(numThreads);
    }

    public LoadGenerator(ExecutorService executor, String serverPath, List<CountDownLatch> latches, RequestCount requestCount,
                         Map<String, BlockingQueue> latencyQueues, int numThreads, String resortID, int dayID, int numLifts,
                         int startSkierID, int endSkierID, int startMinute, int endMinute,
                         boolean coolDown) {
        this(executor, serverPath, latches, requestCount, latencyQueues, numThreads, resortID, dayID, numLifts,
                startSkierID, endSkierID, startMinute, endMinute);
        this.coolDown = coolDown;
    }

    @Override
    public void run() {
        int numSkiers = endSkierID - startSkierID;
        if (coolDown) {
            logger.info("Starting generate loads with LoadWorkerCoolDown");
            for (int i = 0; i < numThreads; i++) {
                RequestCount requestCount = new RequestCount();
                requestCounter.add(requestCount);
                // calculate skierID range for different workers
                int workerStartSkierID = startSkierID + i * (numSkiers / numThreads);
                int workerEndSkierID = startSkierID + (i + 1) * (numSkiers / numThreads);
                LoadWorkerCoolDown worker = new LoadWorkerCoolDown(latches, serverPath, requestCount,
                        latencyQueues, resortID, dayID, numLifts, workerStartSkierID, workerEndSkierID,
                        startMinute, endMinute);
                executor.submit(worker);
            }
        } else {
            logger.info("Starting generate loads with LoadWorker");
            for (int i = 0; i < numThreads; i++) {
                RequestCount requestCount = new RequestCount();
                requestCounter.add(requestCount);
                // calculate skierID range for different workers
                int workerStartSkierID = startSkierID + i * (numSkiers / numThreads);
                int workerEndSkierID = startSkierID + (i + 1) * (numSkiers / numThreads);
                LoadWorker worker = new LoadWorker(latches, serverPath, requestCount,
                        latencyQueues, resortID, dayID, numLifts, workerStartSkierID, workerEndSkierID,
                        startMinute, endMinute);
                executor.submit(worker);
            }
        }
    }

    public void sumRequestCounter() {
        for (RequestCount workerCount : requestCounter) {
            requestCount.success += workerCount.success;
            requestCount.failure += workerCount.failure;
        }
    }
}

class LoadWorker implements Runnable {
    protected List<CountDownLatch> latches;
    protected String serverPath;
    protected RequestCount requestCount;
    protected Map<String, BlockingQueue> latencyQueues;
    protected Random rand;
    protected static Logger logger = LogManager.getLogger(PowderClient.class);

    protected String resortID;
    protected String dayID;
    protected int numLifts;
    protected int startSkierID, endSkierID;
    protected int startMinute, endMinute;

    protected int numPOST = 1000;
    protected int numGET = 5;

    public LoadWorker(List<CountDownLatch> latches, String serverPath, RequestCount requestCount,
                      Map<String, BlockingQueue> latencyQueues, String resortID, String dayID, int numLifts,
                      int startSkierID, int endSkierID, int startMinute, int endMinute) {
        this.latches = latches;
        this.serverPath = serverPath;
        this.requestCount = requestCount;
        this.latencyQueues = latencyQueues;
        this.resortID = resortID;
        this.dayID = dayID;
        this.numLifts = numLifts;
        this.startSkierID = startSkierID;
        this.endSkierID = endSkierID;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        rand = new Random();
    }

    @Override
    public void run() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(serverPath);
        SkiersApi api = new SkiersApi(apiClient);
        logger.info(this.getClass().getName() + "starting to create loads.");
        generatePOSTs(api);
        generateGETs(api);
        for (CountDownLatch latch : latches) {
            latch.countDown();
        }
    }

    protected void generatePOSTs(SkiersApi api) {
        int successCount = 0;
        int failureCount = 0;
        BlockingQueue<String> latencyQueue = latencyQueues.get("writeNewLiftRide");

        for (int i = 0; i < numPOST; i++) {
            try {
                int skierID = startSkierID + rand.nextInt(endSkierID-startSkierID);
                int liftID = rand.nextInt(numLifts) + 1;
                int minute = startMinute + rand.nextInt(endMinute-startMinute);
                LiftRide ride = packLiftRide(liftID, skierID, minute);
                long startNano = System.nanoTime();
                ApiResponse<Void> response = api.writeNewLiftRideWithHttpInfo(ride);
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + "," + latency + "," + responseCode;
                if (!latencyQueue.offer(latencyEntry)) {
                    logger.error("latencyQueue is full, latency entry not logged: " + latencyEntry);
                }
                if (responseCode.startsWith("4") || responseCode.startsWith("5")) {
                    logger.error(this.getClass().getName() + " received status code " + responseCode +
                            " when calling POST writeNewLiftRide");
                    failureCount++;
                } else {
                    successCount++;
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling writeNewLiftRide");
                failureCount++;
                e.printStackTrace();
            }
        }
        requestCount.success += successCount;
        requestCount.failure += failureCount;
    }

    protected void generateGETs(SkiersApi api) {
        int successCount = 0;
        int failureCount = 0;
        BlockingQueue<String> latencyQueue = latencyQueues.get("getSkierDayVertical");

        for (int i = 0; i < numGET; i++) {
            int skierID = startSkierID + rand.nextInt(endSkierID-startSkierID);
            try {
                long startNano = System.nanoTime();
                ApiResponse<SkierVertical> response = api.getSkierDayVerticalWithHttpInfo(resortID, dayID,
                        Integer.toString(skierID));
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + "," + latency + "," + responseCode;
                if (!latencyQueue.offer(latencyEntry)) {
                    logger.error("latencyQueue is full, latency entry not logged: " + latencyEntry);
                }
                if (responseCode.startsWith("4") || responseCode.startsWith("5")) {
                    logger.error(this.getClass().getName() + " received status code " + responseCode +
                            " when calling GET getSkierDayVertical");
                    failureCount++;
                } else {
                    successCount++;
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling getSkierDayVertical");
                failureCount++;
                e.printStackTrace();
            }
        }
        requestCount.success += successCount;
        requestCount.failure += failureCount;
    }

    protected LiftRide packLiftRide(int liftID, int skierID, int minute) {
        LiftRide ride = new LiftRide();
        ride.setDayID(dayID);
        ride.setLiftID(Integer.toString(liftID));
        ride.setResortID(resortID);
        ride.setSkierID(Integer.toString(skierID));
        ride.setTime(Integer.toString(minute));
        return ride;
    }
}

class LoadWorkerCoolDown extends LoadWorker {
    protected int numGET = 10;

    public LoadWorkerCoolDown(List<CountDownLatch> latches, String serverPath, RequestCount requestCount,
                              Map<String, BlockingQueue> latencyQueues, String resortID, String dayID, int numLifts,
                              int startSkierID, int endSkierID, int startMinute, int endMinute) {
        super(latches, serverPath, requestCount, latencyQueues, resortID, dayID, numLifts,
                startSkierID, endSkierID, startMinute, endMinute);
    }

    protected void generateGETs(SkiersApi api) {
        int successCount = 0;
        int failureCount = 0;
        BlockingQueue<String> latencyQueueDay = latencyQueues.get("getSkierDayVertical");
        BlockingQueue<String> latencyQueueResort = latencyQueues.get("getSkierResortTotals");

        for (int i = 0; i < numGET; i++) {
            int skierID = startSkierID + rand.nextInt(endSkierID-startSkierID);
            try {
                long startNano = System.nanoTime();
                ApiResponse<SkierVertical> response = api.getSkierDayVerticalWithHttpInfo(resortID, dayID,
                        Integer.toString(skierID));
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + "," + latency + "," + responseCode;
                if (!latencyQueueDay.offer(latencyEntry)) {
                    logger.error("latencyQueueDay is full, latency entry not logged: " + latencyEntry);
                }
                if (responseCode.startsWith("4") || responseCode.startsWith("5")) {
                    logger.error(this.getClass().getName() + " received status code " + responseCode +
                            " when calling GET getSkierDayVertical");
                    failureCount++;
                } else {
                    successCount++;
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling getSkierDayVertical");
                failureCount++;
                e.printStackTrace();
            }

            List<String> resortIDList = new ArrayList<>();
            resortIDList.add(resortID);
            try {
                long startNano = System.nanoTime();
                ApiResponse<SkierVertical> response = api.getSkierResortTotalsWithHttpInfo(Integer.toString(skierID),
                        resortIDList);
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + "," + latency + "," + responseCode;
                if (!latencyQueueResort.offer(latencyEntry)) {
                    logger.error("latencyQueueResort is full, latency entry not logged: " + latencyEntry);
                }
                if (responseCode.startsWith("4") || responseCode.startsWith("5")) {
                    logger.error(this.getClass().getName() + " received status code " + responseCode +
                            " when calling GET getSkierResortTotals");
                    failureCount++;
                } else {
                    successCount++;
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling getSkierResortTotals");
                failureCount++;
                e.printStackTrace();
            }
        }
        requestCount.success += successCount;
        requestCount.failure += failureCount;
    }
}

