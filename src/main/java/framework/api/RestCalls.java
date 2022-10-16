package framework.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.params.CoreConnectionPNames;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Log4j
public abstract class RestCalls {

    String baseUrl;
    String requestUrl;
    private Response response;
    private ContentType contentType;
    private Map<String, String> headers = new HashMap<>();

    RequestSpecification requestSpecification;
    private PrintStream restAssuredPrintStream;

    /**
     * Changing Restassured underlying object mapper settings to:
     * exclude null values from serialization
     * to avoid writing dates as timestamp and be able to serialize java 8 LocalDate
     * to avoid redirects after making REST calls
     */
    RestAssuredConfig restAssuredConfig() {
        return RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (aClass, s) -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                    objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES,true);
                    return objectMapper;
                }
        )).logConfig(new LogConfig(restAssuredPrintStream, true)).logConfig(new LogConfig(restAssuredPrintStream, true)).httpClient(HttpClientConfig.httpClientConfig()
                .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000000)
                .setParam(CoreConnectionPNames.SO_TIMEOUT, 5000000));
    }

    public Services getRequest(int responseCode) {
        return getRequest(responseCode, null, null);
    }

    public Services getRequest(int responseCode, Map<String, ?> queryParams) {
        return getRequest(responseCode, queryParams, null);
    }

    public Services getRequest(int responseCode, @NonNull RequestSpecification spec) {
        return getRequest(responseCode, null, spec);
    }

    public synchronized Services getRequest(int responseCode, Map<String, ?> queryParams, RequestSpecification spec) {
        log.info("Calling GET: " + requestUrl);
        RequestSpecification requestSpec = RestAssured.given()
                .contentType(ContentType.JSON)
                .headers(headers)
                .queryParams(queryParams != null ? queryParams : new HashMap<>());

        if (spec != null) requestSpec.spec(spec);

        response = requestSpec
                .when()
                .get(requestUrl);

        // log if request failed
        if (response.getStatusCode() != responseCode) {
            logRequest((RequestSpecificationImpl) requestSpec);
        }

        response
                .then()
                .statusCode(responseCode);
        return (Services) this;
    }


    public Services postRequest(Object body, int responseCode) {
        return postRequest(body, responseCode, null);
    }

    public Services postRequest(Object body, Integer responseCode, Map<String, ?> queryParams) {
        return postRequest(body, responseCode, queryParams, null);
    }

    public Services postRequest(Object body, Map<String, ?> queryParams) {
        return postRequest(body, null, queryParams);
    }

    public Services postRequest(int responseCode, RequestSpecification spec) {
        return postRequest(null, responseCode, null, spec);
    }

    public synchronized Services postRequest(Object body, Integer responseCode, Map<String, ?> queryParams, RequestSpecification reqSpec) {
        log.info("Calling POST: " + requestUrl);

        RequestSpecification requestSpecification;
        if (reqSpec != null) {
            requestSpecification = RestAssured.given().spec(reqSpec);
            if (headers != null) {
                requestSpecification.headers(headers);
            }
        } else {

            log.info("Request body:" + body);
            requestSpecification = this.requestSpecification
                    .contentType(contentType)
                    .headers(headers)
                    .queryParams(queryParams != null ? queryParams : new HashMap<>());
            if (body != null) requestSpecification.body(body);
        }

        response = requestSpecification
                .when()
                .post(requestUrl);

        // log if request failed
        if ((responseCode != null && response.getStatusCode() != responseCode) || response.getStatusCode() > 399) {
            logRequest((RequestSpecificationImpl) requestSpecification);
        }
        if (responseCode != null) {
            try {
                response.then().statusCode(responseCode);
            } catch (AssertionError e) {
                if (reqSpec == null) this.requestSpecification = RestAssured.given();
                throw new RuntimeException("Expected response code did not match for a Post request to: " + requestUrl +
                        "\n with status : \n" + response.statusLine(), e);
            }
        }

        if (reqSpec == null) this.requestSpecification = RestAssured.given();
        return (Services) this;
    }

    public synchronized Services putRequest(Object body, int responseCode, Map<String, ?> queryParams) {
        log.info("Calling PUT: " + requestUrl);

        requestSpecification
                .contentType(contentType)
                .headers(headers)
                .queryParams(queryParams != null ? queryParams : new HashMap<>());

        response = requestSpecification
                .body(body).when()
                .put(requestUrl);

        // log if request failed
        if (response.getStatusCode() != responseCode) {
            logRequest((RequestSpecificationImpl) requestSpecification);
        }

        response
                .then()
                .statusCode(responseCode);

        requestSpecification = RestAssured.given();
        return (Services) this;
    }

    public synchronized Services patchRequest(Object body, int responseCode, Map<String, ?> queryParams) {
        log.info("Calling PATCH: " + requestUrl);
        requestSpecification
                .filter((reqSpec, recResponse, context) -> {
                    reqSpec.removeHeaders();
                    reqSpec.headers(headers);
                    reqSpec.contentType(contentType);
                    return context.next(reqSpec, recResponse);
                })
                .queryParams(queryParams != null ? queryParams : new HashMap<>());

        response = requestSpecification
                .body(body, ObjectMapperType.JACKSON_2)
                .when()
                .patch(requestUrl);

        // log if request failed
        if (response.getStatusCode() != responseCode){
            logRequest((RequestSpecificationImpl) requestSpecification);
        }

        response
                .then()
                .statusCode(responseCode);

        requestSpecification = RestAssured.given();
        return (Services) this;
    }

    public synchronized Services deleteRequest(int responseCode) {
        log.info("Calling : " + requestUrl);
        response = RestAssured.given()
                .contentType("application/json")
                .headers(headers)
                .delete(requestUrl);

        // log if request failed
        if (response.getStatusCode() != responseCode) logRequest((RequestSpecificationImpl) RestAssured.requestSpecification);


        response
                .then()
                .statusCode(responseCode);

        return (Services) this;
    }

    public Services addHeader(String key, String value) {
        headers.put(key, value);
        return (Services) this;
    }

    public String getHeader(String key) {
        checkArgument(key != null, "key cant be null");
        return headers.get(key);
    }

    public Services resetHeaders() {
        headers = new HashMap<>();
        return (Services) this;
    }

    public Services setAuthorization(String authorization) {
        removeAuthorization();

        if (!StringUtils.isEmpty(authorization)) {
            headers.put("Authorization", authorization);
        }

        return (Services) this;
    }

    public Services setRequestUrl(String url, Object... parameters) {
        requestUrl = baseUrl == null ? String.format(url, parameters) : baseUrl + String.format(url, parameters);
        return (Services) this;
    }

    public String getRequestUrl(String url, Object... parameters) {
        return requestUrl = baseUrl + String.format(url, parameters);
    }

    public Services resetRequestUrl() {
        requestUrl = baseUrl;
        return (Services) this;
    }

    public Services setContentType(ContentType contentType) {
        this.contentType = contentType;
        return (Services) this;
    }

    public Services setBasicAuth(String userName, String password) {
        checkArgument(userName != null && password != null, "username and password cant be null");

        removeAuthorization();

        requestSpecification
                .auth()
                .preemptive()
                .basic(userName, password);
        return (Services) this;
    }

    Services removeAuthorization() {
        headers.remove("authorization");
        headers.remove("Authorization");
        return (Services) this;
    }

    public String getResponseHeaderVal(String key) {
        return response.getHeader(key);
    }

    public String getResponseValueForJsonPath(String path) {
        return response
                .jsonPath()
                .get(path)
                .toString();
    }

    public String getResponseValueForXmlPath(String path) {
        return response
                .xmlPath()
                .get(path)
                .toString();
    }

    public <T> T getResponseAsObject(Class<T> clazz) {
        return response
                .as(clazz);
    }

    public String getBody() {
        return response.asString();
    }

    public Response getResponse() {
        return response;
    }

    void setPrintStream() {
        if (restAssuredPrintStream == null) {
            OutputStream output = new OutputStream() {
                private StringBuilder myStringBuilder = new StringBuilder();

                @Override
                public void write(int b) {
                    this.myStringBuilder.append((char) b);
                }

                @Override
                public void flush() {
                    if (!myStringBuilder.toString().isEmpty() && !myStringBuilder.toString().equalsIgnoreCase("\n")) {
                        log.info("<pre>" + this.myStringBuilder.toString() + "</pre>");
                    }
                    myStringBuilder = new StringBuilder();
                }
            };
            restAssuredPrintStream = new PrintStream(output, true);  // true: auto-flush must be set!
        }
    }

    /**
     * Used to log failed requests
     */
    private void logRequest(RequestSpecificationImpl reqSpec) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .enable(SerializationFeature.INDENT_OUTPUT);

            String body;
            if (reqSpec.getBody() == null || reqSpec.getBody().equals("")) {
                body = "NULL";
            } else {
                Object json = mapper.readValue((String) reqSpec.getBody(), Object.class);
                body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            }

            List<Header> headers = reqSpec.getHeaders().asList();
            String reqHeaders = headers.isEmpty() ? "NULL" : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(headers);

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n\n----------------------------------------  REQUEST_FAILED  -----------------------------------------------------------------------------");
            logBuilder.append("\n - REQUEST_METHOD: ").append(reqSpec.getMethod());
            logBuilder.append("\n - REQUEST_URL: ").append(reqSpec.getURI());
            logBuilder.append("\n - REQUEST_HEADERS: ").append(reqHeaders);
            logBuilder.append("\n - REQUEST_BODY: ").append(body).append("\n");
            logBuilder.append("\n - RESPONSE_STATUS: ").append(response.getStatusCode());
            logBuilder.append("\n - RESPONSE_BODY: ").append(response.getBody().asString()).append("\n");
            logBuilder.append("\n - RESPONSE_HEADERS:\n").append(response.getHeaders());
            logBuilder.append("\n=====================================================================================================================================\n\n");

            log.info(logBuilder);
        } catch (Exception e) {
            System.out.println();
        }
    }
}
