import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.TopTen;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class LoadGenerator implements Runnable{
    private String serverPath;
    private List<CountDownLatch> latches;
    private int numThreads;
    private int startSkierId, endSkierId;
    private int startMinute, endMinute;
    private boolean coolDown = false;

    public LoadGenerator(String serverPath, List<CountDownLatch> latches,
                         int numThreads, int startSkierId, int endSkierId,
                         int startMinute, int endMinute) {
        this.serverPath = serverPath;
        this.latches = latches;
        this.numThreads = numThreads;
        this.startSkierId = endSkierId;
        this.endSkierId = endSkierId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public LoadGenerator(String serverPath, List<CountDownLatch> latches,
                         int numThreads, int startSkierId, int endSkierId,
                         int startMinute, int endMinute, boolean coolDown) {
        this(serverPath, latches, numThreads, startSkierId, endSkierId, startMinute, endMinute);
        this.coolDown = coolDown;
    }

    @Override
    public void run() {
        if (coolDown) {
            for (int i = 0; i < numThreads; i++) {
                LoadWorkerCoolDown worker = new LoadWorkerCoolDown(latches, serverPath);
                new Thread(worker).start();
            }
        } else {
            for (int i = 0; i < numThreads; i++) {
                LoadWorker worker = new LoadWorker(latches, serverPath);
                new Thread(worker).start();
            }
        }
    }
}

class LoadWorker implements Runnable {
    protected List<CountDownLatch> latches;
    protected String serverPath;

    public LoadWorker(List<CountDownLatch> latches, String serverPath) {
        this.latches = latches;
        this.serverPath = serverPath;
    }

    @Override
    public void run() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(serverPath);
        SkiersApi api = new SkiersApi(apiClient);
        generatePOST(api);
        generateGET(api);
        for (int i = 0; i < latches.size(); i++) {
            latches.get(i).countDown();
        }
    }

    protected void generatePOST(SkiersApi api) {
        for (int i = 0; i < 100; i++) {
            try {
                TopTen result = apiInstance.getTopTenVert(resort, dayID);
                System.out.println(result);
            } catch (ApiException e) {
                System.err.println("Exception when calling ResortsApi#getTopTenVert");
                e.printStackTrace();
            }
        }
    }

    protected void generateGET(SkiersApi api) {
        for (int i = 0; i < 10; i++) {
            System.out.println("POST");
        }
    }
}

class LoadWorkerCoolDown extends LoadWorker {

    public LoadWorkerCoolDown(List<CountDownLatch> latches, String serverPath) {
        super(latches, serverPath);
    }

    protected void generateGET(SkiersApi api) {
        for (int i = 0; i < 5; i++) {
            System.out.println("POST_CD");
        }
    }
}

