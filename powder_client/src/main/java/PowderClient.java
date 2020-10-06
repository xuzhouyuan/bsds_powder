import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.util.*;

public class PowderClient {


    public static void main(String[] args) {
        ResortsApi apiInstance = new ResortsApi();
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
