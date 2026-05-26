package wwn.backend.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.User;
import wwn.backend.domain.UserRoleType;
import wwn.backend.dto.request.UserRequestDTO;
import wwn.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * 로그인 유저 유효성 체크
     */
    @Transactional(readOnly = true)
    public Boolean existEmail(UserRequestDTO dto) {
        return userRepository.existsByEmail(dto.getEmail());
    }

    /**
     * 회원가입
     */
    @Transactional
    public Long addUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }
        User userEntity = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .role(UserRoleType.USER)
                .isSocial(false)
                .build();

        return userRepository.save(userEntity).getId();
    }

    /**
     * 자체로그인
     */
    @Transactional
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User userEntity = userRepository.findByEmailAndIsSocial(email, false)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return org.springframework.security.core.userdetails.User //스프링 시큐리티 내부 User 객체 도메인 User 아님.
                .builder()
                .username(userEntity.getEmail())
                .password(userEntity.getPassword())
                .roles(userEntity.getUserRoleType().name())
                .build();
    }

    /**
     * 로그인 사용자 정보 수정
     */
    @Transactional
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {
        // 본인만 수정 가능
        String sessionUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (sessionUserEmail.equals(dto.getEmail())) {
            throw new AccessDeniedException("본인 계정만 수정 가능.");
        }

        //조회
        User userEntity = userRepository.findByEmailAndIsSocial(dto.getEmail(), false)
                .orElseThrow(() -> new UsernameNotFoundException(dto.getEmail()));

        //회원 정보 수정
        userEntity.updateUser(dto);

        return userRepository.save(userEntity).getId();
    }
    /**
     * 회원 탈퇴
     */

    /**
     * 소셜 로그인
     */


    /**
     * 로그인 정보 확인
     */
}
