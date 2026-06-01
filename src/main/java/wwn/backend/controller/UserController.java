package wwn.backend.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wwn.backend.dto.request.UserRequestDTO;
import wwn.backend.dto.response.UserResponseDTO;
import wwn.backend.service.UserService;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    /**
     * 회원가입 API
     * JSON 형태의 요청 데이터를 받아 회원가입 처리 후 생성된 유저 ID 반환함.
     */
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated(UserRequestDTO.addGroup.class) // 앞서 정의한 회원가입 전용 유효성 검증 작동
            @RequestBody UserRequestDTO dto) {

        // 서비스 계층 호출하여 회원가입 진행 후 저장된 PK(ID) 식별자 반환받음
        Long id = userService.addUser(dto);

        // 클라이언트에게 반환할 응답 바디를 Key-Value 구조의 불변 맵으로 생성함
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);

        // HTTP 상태 코드 201(Created)과 함께 데이터 즉시 반환함
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * 내 정보
     */
    @GetMapping(value = "/info", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO getUserInfo() {
        return userService.readUser();
    }

    /**
     * 유저 정보 수정
     */
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserInfo(
            @Validated(UserRequestDTO.updateGroup.class)
            @RequestBody UserRequestDTO dto
    ) throws UsernameNotFoundException {

        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(dto));

    }

    /**
     * 유저 탈퇴 , 제거
     */
    @DeleteMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteUserInfo(
            @Validated(UserRequestDTO.deleteGroup.class)
            @RequestBody UserRequestDTO dto
    ) throws AccessDeniedException {

        userService.deleteUser(dto);
        return ResponseEntity.status(HttpStatus.OK).body(true);
    }
}
