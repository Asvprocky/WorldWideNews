package wwn.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTUtil {

    private static final SecretKey secretKey;
    private static final Long accessTokenExpire;
    private static final Long refreshTokenExpire;

    static {
        String secretKeyString = "qweasdfdqwrqgfdsawefvdkfkewkerkr";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

        accessTokenExpire = 10800L * 1000; // 3시간
        refreshTokenExpire = 604800L * 1000; // 7일

    }

    /**
     * JWT 클레임 username 파싱
     */
    public static String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("sub", String.class);
    }

    /**
     * JWT 클레임 role 파싱
     */
    public static String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }


    /**
     * JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
     * isAccess가 true일 시 Access 토큰 검증, false일 시 Refresh 토큰 검증함
     */
    public static Boolean isValid(String token, Boolean isAccess) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // String.class = 안전하게 문자열 타입으로 자동변환 해서 가져옴
            String type = claims.get("type", String.class);
            if (type == null) return false;

            // 삼항 연산자나 조건문을 더 간결하게 합쳐서 판단함
            if (isAccess && !type.equals("access")) return false;
            if (!isAccess && !type.equals("refresh")) return false;

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰 위조, 만료시간 초과 시 모두 여기서 캐치되어 false 반환함
            return false;
        }
    }

    /**
     * JWT 토큰 생성(access, refresh)
     * Boolean isAccess 가 true 일시 accessToken 생성 false 일시 refreshToken 생성
     */
    public static String createJWT(String username, String role, Boolean isAccess) {
        long now = System.currentTimeMillis();
        long expire = isAccess ? accessTokenExpire : refreshTokenExpire;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                .claim("sub", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expire))
                .signWith(secretKey)
                .compact();
    }
}
