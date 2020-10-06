import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;

public class PowderClient {
    private String serverPath = "http://localhost:8080/powder";

    public static void main(String[] args) {
        // initalize client and set server path
        PowderClient client = new PowderClient();
        // TODO: set server path from command line input

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
}
