package framework.api;

import framework.api.requests.CreateUserRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static com.google.common.base.Preconditions.checkArgument;

public class UserService {

    private Services api;

    public UserService(Services services) {
        this.api = services;
    }

    public Response postUser(CreateUserRequest user, Integer statusCode) {

        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users")
                .postRequest(user,statusCode)
                .getResponse();
    }

    public Response putUser(String id, CreateUserRequest userRequest, Integer statusCode) {

        checkArgument(id != null,"Parameters cant be null");
        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users/%s",id)
                .putRequest(userRequest,statusCode,null)
                .getResponse();
    }

    public Response deleteUser(String id, Integer statusCode) {

        checkArgument(id != null,"Parameters cant be null");
        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users/%s",id)
                .deleteRequest(statusCode)
                .getResponse();
    }
}
