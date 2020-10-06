import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;

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

    public static void main(String[] args) {
        // initialize client and set server path
        PowderClient client = new PowderClient();

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(client.serverPath);

        ResortsApi apiInstance = new ResortsApi(apiClient);
        List<String> resort = Arrays.asList("resort_example"); // List<String> | resort to query by
        List<String> dayID = Arrays.asList("dayID_example"); // List<String> | day number in the season
        try {
            TopTen result = apiInstance.getTopTenVert(resort, dayID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ResortsApi#getTopTenVert");
            e.printStackTrace();
        }
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
