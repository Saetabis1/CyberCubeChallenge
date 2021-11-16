package cybercubeChallenge.api;

import cybercubeChallenge.api.requests.CreateUserRequest;
import cybercubeChallenge.api.responses.CreateUserResponse;
import cybercubeChallenge.api.responses.UpdateUserResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static com.google.common.base.Preconditions.checkArgument;

public class UserService {

    private Services api;

    public UserService(Services services) {
        this.api = services;
    }

    public Response postUser(CreateUserRequest user) {

        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users")
                .postRequest(user,201)
                .getResponse();
    }

    public Response putUser(String id, CreateUserRequest userRequest) {

        checkArgument(id != null,"Parameters cant be null");
        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users/%s",id)
                .putRequest(userRequest,200,null)
                .getResponse();
    }

    public Response deleteUser(String id) {

        checkArgument(id != null,"Parameters cant be null");
        return api
                .setContentType(ContentType.JSON)
                .setRequestUrl("api/users/%s",id)
                .deleteRequest(204)
                .getResponse();
    }
}
