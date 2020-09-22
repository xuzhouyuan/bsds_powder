# ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getTopTenVert**](ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi.md#getTopTenVert) | **GET** /resort/day/top10vert | get the top 10 skier vertical totals for this day

<a name="getTopTenVert"></a>
# **getTopTenVert**
> TopTen getTopTenVert()

get the top 10 skier vertical totals for this day

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi;


ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi apiInstance = new ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi();
try {
    TopTen result = apiInstance.getTopTenVert();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ReturnTopTenSkierVerticalTotalsForTheSpecifiedResortdayApi#getTopTenVert");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**TopTen**](TopTen.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

