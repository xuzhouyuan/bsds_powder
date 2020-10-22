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
import java.util.Random;
import java.util.concurrent.*;

public class LoadGenerator implements Runnable{
    private String serverPath;
    private List<CountDownLatch> latches;
    private String resortID;
    private String dayId;
    private int numThreads;
    private int numLifts;
    private int startSkierId, endSkierId;
    private int startMinute, endMinute;
    private boolean coolDown = false;

    private ExecutorService executor;

    private List<RequestCount> requestCounter;
    private RequestCount requestCount;
    private BlockingQueue<String> latencyQueue;
    private static Logger logger = LogManager.getLogger(PowderClient.class);

    public LoadGenerator(ExecutorService executor, String serverPath, List<CountDownLatch> latches, RequestCount requestCount,
                         BlockingQueue<String> latencyQueue, int numThreads,String resortID, int dayId, int numLifts,
                         int startSkierId, int endSkierId, int startMinute, int endMinute) {
        this.executor = executor;
        this.serverPath = serverPath;
        this.latches = latches;
        this.requestCount = requestCount;
        this.latencyQueue = latencyQueue;
        this.numThreads = numThreads;
        this.resortID = resortID;
        this.dayId = Integer.toString(dayId);
        this.numLifts = numLifts;
        this.startSkierId = startSkierId;
        this.endSkierId = endSkierId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        requestCounter = new ArrayList<>(numThreads);
    }

    public LoadGenerator(ExecutorService executor, String serverPath, List<CountDownLatch> latches, RequestCount requestCount,
                         BlockingQueue<String> latencyQueue, int numThreads, String resortID, int dayId, int numLifts,
                         int startSkierId, int endSkierId, int startMinute, int endMinute,
                         boolean coolDown) {
        this(executor, serverPath, latches, requestCount, latencyQueue, numThreads, resortID, dayId, numLifts,
                startSkierId, endSkierId, startMinute, endMinute);
        this.coolDown = coolDown;
    }

    @Override
    public void run() {
        int numSkiers = endSkierId - startSkierId;
        if (coolDown) {
            logger.info("Starting generate loads with LoadWorkerCoolDown");
            for (int i = 0; i < numThreads; i++) {
                RequestCount requestCount = new RequestCount();
                requestCounter.add(requestCount);
                // calculate skierId range for different workers
                int workerStartSkierId = startSkierId + i * (numSkiers / numThreads);
                int workerEndSkierId = startSkierId + (i + 1) * (numSkiers / numThreads);
                // TODO change to use executor and then compare walltime
                LoadWorkerCoolDown worker = new LoadWorkerCoolDown(latches, serverPath, requestCount,
                        latencyQueue, resortID, dayId, numLifts, workerStartSkierId, workerEndSkierId,
                        startMinute, endMinute);
                executor.submit(worker);
            }
        } else {
            logger.info("Starting generate loads with LoadWorker");
            for (int i = 0; i < numThreads; i++) {
                RequestCount requestCount = new RequestCount();
                requestCounter.add(requestCount);
                // calculate skierId range for different workers
                int workerStartSkierId = startSkierId + i * (numSkiers / numThreads);
                int workerEndSkierId = startSkierId + (i + 1) * (numSkiers / numThreads);
                LoadWorker worker = new LoadWorker(latches, serverPath, requestCount,
                        latencyQueue, resortID, dayId, numLifts, workerStartSkierId, workerEndSkierId,
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
    protected BlockingQueue<String> latencyQueue;
    protected Random rand;
    protected static Logger logger = LogManager.getLogger(PowderClient.class);

    protected String resortId;
    protected String dayId;
    protected int numLifts;
    protected int startSkierId, endSkierId;
    protected int startMinute, endMinute;

    protected int numPOST = 100;
    protected int numGET = 5;

    public LoadWorker(List<CountDownLatch> latches, String serverPath, RequestCount requestCount,
                      BlockingQueue<String> latencyQueue, String resortId, String dayId, int numLifts,
                      int startSkierId, int endSkierId, int startMinute, int endMinute) {
        this.latches = latches;
        this.serverPath = serverPath;
        this.requestCount = requestCount;
        this.latencyQueue = latencyQueue;
        this.resortId = resortId;
        this.dayId = dayId;
        this.numLifts = numLifts;
        this.startSkierId = startSkierId;
        this.endSkierId = endSkierId;
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
        for (int i = 0; i < numPOST; i++) {
            try {
                int skierId = startSkierId + rand.nextInt(endSkierId-startSkierId);
                int liftId = rand.nextInt(numLifts) + 1;
                int minute = startMinute + rand.nextInt(endMinute-startMinute);
                LiftRide ride = packLiftRide(liftId, skierId, minute);
                long startNano = System.nanoTime();
                ApiResponse<Void> response = api.writeNewLiftRideWithHttpInfo(ride);
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + ",POST," + latency + "," + responseCode;
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
        for (int i = 0; i < numGET; i++) {
            try {
                int skierId = startSkierId + rand.nextInt(endSkierId-startSkierId);
                long startNano = System.nanoTime();
                ApiResponse<SkierVertical> response = api.getSkierDayVerticalWithHttpInfo(resortId, dayId,
                        Integer.toString(skierId));
                long endNano = System.nanoTime();
                int latency = (int) ((endNano - startNano) / 1000000); // millisecond
                String responseCode = Integer.toString(response.getStatusCode());
                String latencyEntry = startNano + ",GET," + latency + "," + responseCode;
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

    protected LiftRide packLiftRide(int liftId, int skierId, int minute) {
        LiftRide ride = new LiftRide();
        ride.setDayID(dayId);
        ride.setLiftID(Integer.toString(liftId));
        ride.setResortID(resortId);
        ride.setSkierID(Integer.toString(skierId));
        ride.setTime(Integer.toString(minute));
        return ride;
    }
}

class LoadWorkerCoolDown extends LoadWorker {

    public LoadWorkerCoolDown(List<CountDownLatch> latches, String serverPath, RequestCount requestCount,
                              BlockingQueue<String> latencyQueue, String resortId, String dayId, int numLifts,
                              int startSkierId, int endSkierId, int startMinute, int endMinute) {
        super(latches, serverPath, requestCount, latencyQueue, resortId, dayId, numLifts,
                startSkierId, endSkierId, startMinute, endMinute);
        numGET = 10;
    }
}

