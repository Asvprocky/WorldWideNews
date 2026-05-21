package wwn.backend.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {

    private String email;

    private String password;

    private String nickname;

}
