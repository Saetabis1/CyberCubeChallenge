package cybercubeChallenge.api;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;

import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;

public class Services extends RestCalls {

    private UserService userService;

    public Services() {
        setPrintStream();
        RestAssuredConfig conf = restAssuredConfig();
        conf = conf.redirect(redirectConfig().followRedirects(false));
        conf = conf.encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));

        RestAssured.config = conf;
        requestSpecification = RestAssured.given();
        setContentType(ContentType.JSON);
    }

    public UserService getUserService() {
        if (userService == null) userService = new UserService(this);
        resetHeaders();
        baseUrl = "https://reqres.in/";
        return userService;
    }
}
