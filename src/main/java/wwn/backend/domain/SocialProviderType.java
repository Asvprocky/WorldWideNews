package wwn.backend.domain;

import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocialProviderType {
    KAKAO, NAVER, GOOGLE
}
