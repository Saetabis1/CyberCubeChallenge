package cybercubeChallenge;

import cybercubeChallenge.api.requests.CreateUserRequest;
import cybercubeChallenge.api.responses.CreateUserResponse;
import cybercubeChallenge.api.responses.UpdateUserResponse;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Log4j
public class ApiTests extends BaseTest{

    @Test
    public void verifyUserCreation () {

        Random r = new Random();

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("manuelUsername" + r.nextInt(999))
                .job("randomJob" + r.nextInt(999))
                .build();

        // Creates User, Response code is checked in the method
        CreateUserResponse createUserResponse = api.getUserService()
                .postUser(userRequest)
                .as(CreateUserResponse.class);

        assertEquals(createUserResponse.getName(),userRequest.getName());
        assertEquals(createUserResponse.getJob(),userRequest.getJob());
    }

    @Test
    public void verifyUserUpdate () {

        Random r = new Random();

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("manuelUsername" + r.nextInt(999))
                .job("randomJob" + r.nextInt(999))
                .build();

        // Creates User
        CreateUserResponse createUserResponse = api.getUserService()
                .postUser(userRequest)
                .as(CreateUserResponse.class);

        // UpdateUser, Response code is checked in the method
        userRequest.setJob("updatedJob" + r.nextInt(999));
        UpdateUserResponse updateUserResponse = api.getUserService()
                .putUser(createUserResponse.getId().toString(), userRequest)
                .as(UpdateUserResponse.class);

        assertEquals(updateUserResponse.getJob(),userRequest.getJob());
    }

    @Test
    public void verifyUserDeletion() {

        Random r = new Random();

        CreateUserRequest userRequest = CreateUserRequest.builder()
                .name("manuelUsername" + r.nextInt(999))
                .job("randomJob" + r.nextInt(999))
                .build();

        // Creates User
        CreateUserResponse createUserResponse = api.getUserService()
                .postUser(userRequest)
                .as(CreateUserResponse.class);

        // Response code is checked in the method
        api.getUserService().deleteUser(createUserResponse.getId().toString());
    }
}
