package wwn.backend.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {

    // 로그인 ID (이메일)
    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class})
    @Email(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class})
    private String email;

    // 비밀번호
    @NotBlank(groups = {addGroup.class, passwordGroup.class})
    @Size(min = 4, groups = {addGroup.class, passwordGroup.class}) // addGroup과 passwordGroup일 때만 4글자 이상 검사함
    private String password;

    // 닉네임
    @NotBlank(groups = {addGroup.class, updateGroup.class})
    @Size(min = 2, max = 20, groups = {addGroup.class, updateGroup.class}) // addGroup과 updateGroup일 때만 글자수 검사함
    private String nickname;


    // 검증 그룹 인터페이스들 정의
    public interface existGroup {
    }    // 로그인 또는 존재 여부 확인 시 사용

    public interface addGroup {
    }      // 회원가입(등록) 시 사용

    public interface passwordGroup {
    } // 비밀번호 변경 시 사용

    public interface updateGroup {
    }   // 회원정보 수정 시 사용

    public interface deleteGroup {
    }   // 회원탈퇴(삭제) 시 사용


}
