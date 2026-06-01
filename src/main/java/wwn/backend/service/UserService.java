package wwn.backend.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwn.backend.domain.SocialProviderType;
import wwn.backend.domain.User;
import wwn.backend.domain.UserRoleType;
import wwn.backend.dto.oauth2.CustomOAuth2User;
import wwn.backend.dto.request.UserRequestDTO;
import wwn.backend.dto.response.UserResponseDTO;
import wwn.backend.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService extends DefaultOAuth2UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

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
    @Transactional
    public void deleteUser(UserRequestDTO dto) throws AccessDeniedException {
        SecurityContext context = SecurityContextHolder.getContext();
        String sessionUserEmail = context.getAuthentication().getName();
        String role = context.getAuthentication().getAuthorities().iterator().next().getAuthority();

        boolean isOwner = sessionUserEmail.equals(dto.getEmail());
        boolean isAdmin = role.equals("ROLE_" + UserRoleType.ADMIN.name());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("본인만 삭제 할 수있습니다.");
        }
        // 유저 제거
        userRepository.deleteByEmail(dto.getEmail());

        // jwt refresh 토큰 제거
        jwtService.deleteByEmail(dto.getEmail());
    }

    /**
     * 소셜 로그인
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 부모 메서드에게 userRequest 받아서 넘겨줌
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 데이터
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        // [변수 선언부]
        String email;    // 실제 소셜 계정의 이메일 주소
        String nickname; // 소셜 계정의 닉네임/이름
        String role = UserRoleType.USER.name(); // 기본 권한은 USER로 고정

        // 소셜 로그인 제공자(NAVER, GOOGLE 등) 이름 획득 및 대문자 변환
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();

        if (registrationId.equals(SocialProviderType.NAVER.name())) {
            // 네이버는 유저 정보가 "response"라는 Map 안에 한 번 더 감싸져 있음
            attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");

            email = attributes.get("email").toString();
            nickname = attributes.get("nickname").toString();
            // 일반 로그인 규격과의 통일을 위해 username 자리에 진짜 이메일 주소 대입함

        } else if (registrationId.equals(SocialProviderType.GOOGLE.name())) {
            // 구글은 최상위 Attributes에 데이터가 바로 평평하게(Flat) 들어있음
            attributes = (Map<String, Object>) oAuth2User.getAttributes();

            email = attributes.get("email").toString();
            nickname = attributes.get("name").toString();
            // 일반 로그인 규격과의 통일을 위해 username 자리에 진짜 이메일 주소 대입함

        } else {
            // 지원하지 않는 플랫폼일 경우 예외를 던지고 소셜 로그인 강제 중단함
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // 1. [규격 통일] 기존의 username 변수 대신 실제 이메일(email) 변수를 기준으로 DB 유저 조회함
        Optional<User> entity = userRepository.findByEmailAndIsSocial(email, true);


        if (entity.isPresent()) {
            // [기존 유저 로그인 단계]
            // DB에 저장되어 있던 실제 권한(Role)을 꺼내어 변수에 대입함
            role = entity.get().getUserRoleType().name();

            // 카카오/네이버 등에서 프로필 별명이나 이메일이 변경되었을 수 있으므로 기존 유저 정보 업데이트함
            UserRequestDTO dto = new UserRequestDTO();
            dto.setNickname(nickname);
            dto.setEmail(email);
            entity.get().updateUser(dto);

            // 변경된 소셜 회원 정보를 DB에 영구 반영(더티 체킹이 작동한다면 save 생략 가능함)
            userRepository.save(entity.get());
        } else {
            // [신규 유저 자동 회원가입 단계]
            // 비밀번호는 소셜 로그인이므로 빈 문자열("") 혹은 임의의 고유값 처리함
            User newUserEntity = User.builder()
                    .email(email)
                    .password("")
                    .isSocial(true)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .role(UserRoleType.USER) // 최초 가입 시 기본 유저 권한 부여함
                    .nickname(nickname)
                    .build();

            userRepository.save(newUserEntity);
        }

        // 2. [권한 매핑] 시큐리티 규격에 맞게 ROLE_ 접두사가 붙은GrantedAuthority 객체 생성함
        // 만약 role 변수에 "ROLE_"이 없다면 "ROLE_" + role 형태로 생성해 주어야 안전함
        // String securityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        authorities = List.of(new SimpleGrantedAuthority(role));

        // 3. [최종 반환] 성공 핸들러로 넘겨줄 CustomOAuth2User 객체 빌드 및 반환함
        // 세 번째 인자인 username 자리에도 프로젝트 규격인 email 변수를 넘겨서 통일성 유지함
        return new CustomOAuth2User(attributes, authorities, email);
    }


    /**
     * 로그인 정보 확인
     */
    @Transactional(readOnly = true)
    public UserResponseDTO readUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다." + email));

        return new UserResponseDTO(email, userEntity.isSocial(), userEntity.getNickname());

    }
}
