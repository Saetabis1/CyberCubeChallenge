package framework.api.responses;

import lombok.Data;

@Data
public class UpdateUserResponse {

    private String name;

    private String job;

    private String updatedAt;
}
