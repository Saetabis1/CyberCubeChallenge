package framework.api.responses;

import lombok.Data;

@Data
public class CreateUserResponse {

    private Integer id;

    private String name;

    private String job;

    private String createdAt;
}
