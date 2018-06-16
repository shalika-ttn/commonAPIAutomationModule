package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class APIHelper {

    private static RequestSpecification prepareRequestParams(Map<String, String> queryParams, List<Header> headers, String body, ContentType contentType) {
        RequestSpecification request = RestAssured.given();
        if (null != queryParams) {
            for (Map.Entry<String, String> keyValue : queryParams.entrySet()) {
                request.param(keyValue.getKey(), keyValue.getValue());
            }
        }

        if (headers!=null)
            request.headers(new Headers(headers));
        if (body != null && !body.isEmpty()) {
            request.body(body);
        }
        if (contentType != null)
            request.contentType(contentType);

        return request;
    }



    /**
     * Generic method to fetch response for different type of API request
     *
     * @param requestUrl
     * @param requestType
     * @param body
     * @param queryParams
     * @param headers
     * @param checkStatus
     * @return
     */
    public static Response fetchApiResponse(String requestUrl, String requestType, String body, Map<String, String> queryParams, List<Header> headers, ContentType contentType, boolean checkStatus) {

        Response apiResponse = null;
        String requestLog = null;
        RequestSpecification apiRequest = prepareRequestParams(queryParams, headers, body, contentType);

        requestLog = "Request body: " + body;

        if (queryParams != null)
            requestLog = requestLog + "Query Params:" + queryParams.toString();

        switch (requestType) {
            case "GET":
                apiResponse = apiRequest.get(requestUrl);
                break;
            case "POST":
                apiResponse = apiRequest.post(requestUrl);
                break;
            case "PATCH":
                apiResponse = apiRequest.patch(requestUrl);
                break;
            case "DELETE":
                apiResponse = apiRequest.delete(requestUrl);
                break;
            default:
                apiResponse = apiRequest.head(requestUrl);
                break;
        }

        long responseTime = apiResponse.getTime();
        if (checkStatus && apiResponse.getStatusCode() != 200 && apiResponse.getStatusCode() != 201) {
            System.out.println("HTTP Status code is :"+ Integer.toString(apiResponse.getStatusCode())+ "HTTP status check failure");
            Assert.assertTrue(false);
        }
        return apiResponse;
    }

    public static Response postResponseWithHeaders(String body, List<Header> headers, ContentType contentType, String url, boolean checkStatus) {
        return fetchApiResponse(url, "POST", body, (Map)null, headers, contentType, checkStatus);
    }



    public static String getValueOfProperty(String filePath, String keyName) {
        try {
            Properties prop = new Properties();
            InputStream inpStream = null;
            inpStream = new FileInputStream(filePath);
            prop.load(inpStream);
            return prop.getProperty(keyName);
        } catch (IOException e) {
            System.out.println("Filepath mentioned is not correct and method breaks with exception " + e.getStackTrace());
            return null;
        }
    }

    /**
     * This method convert a particular object to JSON. This is mostly used for
     * comparator.
     *
     * @param obj
     * @return
     */
    public static String convertToJson(Object obj) {
        Gson gsonConverter = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gsonConverter.toJson(obj);
    }



    public static String generateAPIUrl(String configFile, String serverInitials, String endpoint) {
        String serverIp = getValueOfProperty(configFile, serverInitials);
        String serviceUrl;
        if (endpoint != null) {
            serviceUrl = serverIp + endpoint;
        } else {
            serviceUrl = serverIp;
        }

        return serviceUrl;
    }

    public static <T> T convertFromJson(String jsonData, Class<T> classType) {
        Gson gsonConverter = (new GsonBuilder()).disableHtmlEscaping().setPrettyPrinting().create();
        return gsonConverter.fromJson(jsonData, classType);
    }


    public  Response normalPost( List<Header> header,String body,String url)
    {

        RestAssuredConfig rac = RestAssured.config().sslConfig(new SSLConfig().allowAllHostnames().relaxedHTTPSValidation("TLS"));

        RequestSpecification requestSpecification = RestAssured.given().config(rac);

        requestSpecification.headers(new Headers(header));
        requestSpecification.body(body);
        requestSpecification.baseUri(url);
       Response response= requestSpecification.post();

       return response;

    }

    /**
     * This method will be used for getting response of GET request w/o headers.
     * This method can pass/fail non-200 HTTP status code based on checkStatus field
     * value as false/true.
     *
     * @param queryParams
     * @param url
     * @param checkStatus
     * @return
     */
    public static Response getResponse(Map<String, String> queryParams, String url, boolean checkStatus) {
        return getResponseWithHeaders(queryParams, null, url, checkStatus);
    }

    /**
     * This method will be used for getting response of GET request with headers.
     * This method will fail non 200 HTTP response.
     *
     * @param queryParams
     * @param headers
     * @param url
     * @param checkStatus
     * @return
     */
    public static Response getResponseWithHeaders(Map<String, String> queryParams, List<Header> headers, String url,
                                                  boolean checkStatus) {
        return fetchApiResponse(url, "GET", null, queryParams, headers, null, checkStatus);
    }




    public static String decryptAPIResponse(String key, String encrypted) {
        try {
            KeySpec keySpec = new DESKeySpec(key.getBytes());
            SecretKey secretKey = SecretKeyFactory.getInstance("DES").generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(2, secretKey);
            String encryptedData = encrypted.replaceAll("\"", "");
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(original);
        } catch (Exception var7) {
            System.out.println("Error in decrypting API response. Key = " + key + " Response = " + encrypted);
            return "";
        }
    }
}
