package com.devtie.devteria.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.devtie.devteria.dto.request.AuthenticationRequest;
import com.devtie.devteria.dto.request.IntrospectRequest;
import com.devtie.devteria.dto.request.LogoutRequest;
import com.devtie.devteria.dto.request.RefreshRequest;
import com.devtie.devteria.dto.response.AuthenticationResponse;
import com.devtie.devteria.dto.response.IntrospectResponse;
import com.devtie.devteria.entity.InvalidatedToken;
import com.devtie.devteria.entity.User;
import com.devtie.devteria.exception.AppException;
import com.devtie.devteria.exception.ErrorCode;
import com.devtie.devteria.repository.InvalidatedTokenRepository;
import com.devtie.devteria.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor // Tạo constructor tự động với tất cả các trường thay cho @Autowired
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // gán mặc định cho các trường là final và private
public class AuthencationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt_secret}")
    protected String SECRET_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    private long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    private long REFRESHABLE_DURATION;

    // kiem tra xem người dùng có tồn tại trong hệ thống hay không va nếu có thì so
    // sánh mật khẩu
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authenticating user: {}", SECRET_KEY);
        var user = userRepository
                .findByUserName(request.getUserName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassWord(), user.getPassWord());
        if (!authenticated) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        var token = generateToken(user);
        return AuthenticationResponse.builder().authenticated(true).token(token).build();
    }

    private String generateToken(User user) {
        // header chứa thông tin về thuật toán mã hóa và các thông tin khác
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        // payload chứa các thông tin về người dùng và thời gian hết hạn của token
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName()) // Chủ thể của token, thường là tên người dùng hoặc ID người dùng
                .issuer("devteria")
                .issueTime(new Date()) // Thời gian phát hành token
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString()) // UUID là 1 chuỗi gôm 32 kí tự và k trùng
                .claim("scope", buildScope(user)) // Thêm các claim tùy chỉnh nếu cần
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        // Ky kết token bằng khóa bí mật
        try {
            jwsObject.sign(new MACSigner(SECRET_KEY.getBytes()));
            return jwsObject.serialize(); // Trả về token đã ký
        } catch (JOSEException e) {
            log.error("Error create JWT", e);
            throw new RuntimeException("Error signing JWT", e);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verityToken(token, false);
        } catch (AppException ex) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid) // Kiểm tra xem token có hợp lệ và chưa hết hạn
                .build();
    }

    private SignedJWT verityToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        // Kiểm tra còn hiệu lực hay không
        JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expirationTime = (isRefresh)
                ? new Date(signedJWT
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier); // trả về true nếu token hợp lệ, false nếu không hợp lệ
        if (!(verified && expirationTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        // kiểm tra logout chưa
        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHORIZED);

        return signedJWT;
    }

    // Xây dựng chuỗi scope xuất các vai trò của người dùng
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });

        return stringJoiner.toString();
    }

    public void logout(LogoutRequest request) throws JOSEException, ParseException {

        try {
            var signToken = verityToken(request.getToken(), true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token alreadly expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws JOSEException, ParseException {
        // kiểm tra xem token còn hiệu lực k
        var signJWT = verityToken(request.getToken(), true);

        // thực hiện refresh
        // lấy id toke
        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();
        // loguot cho token cu
        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
        invalidatedTokenRepository.save(invalidatedToken);

        // get User
        var username = signJWT.getJWTClaimsSet().getSubject();
        var user =
                userRepository.findByUserName(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // Cấp lại token mới
        var token = generateToken(user);
        return AuthenticationResponse.builder().authenticated(true).token(token).build();
    }
}
