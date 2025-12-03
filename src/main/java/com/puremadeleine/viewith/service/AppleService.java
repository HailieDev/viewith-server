package com.puremadeleine.viewith.service;

import com.puremadeleine.viewith.config.client.AppleOAuthProperties;
import com.puremadeleine.viewith.dto.client.AppleJwtKeyDto;
import com.puremadeleine.viewith.dto.client.UpdateTokenResDto;
import com.puremadeleine.viewith.exception.ViewithErrorCode;
import com.puremadeleine.viewith.exception.ViewithException;
import com.puremadeleine.viewith.repository.client.AppleAuthApiRepository;
import com.puremadeleine.viewith.util.JwtUtil;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppleService {

    static String GRANT_TYPE_KEY = "grant_type";
    static String GRANT_TYPE_VALUE_REFRESH_TOKEN = "refresh_token";
    static String GRANT_TYPE_VALUE_AUTH_KEY = "authorization_code";

    static String CLIENT_ID_KEY = "client_id";
    static String CLIENT_SECRET_KEY = "client_secret";
    static String AUTH_CODE_KEY = "code";
    static String REFRESH_TOKEN_KEY = "refresh_token";
    static String TOKEN_TYPE_HINT_KEY = "token_type_hint";

    AppleAuthApiRepository appleAuthApiRepository;
    AppleOAuthProperties appleProperties;
    PrivateKey appleOAuthPrivateKey;

    public UpdateTokenResDto validateAuthCode(String authCode) {
        try {
            Map<String, String> form = new HashMap<>();
            form.put(GRANT_TYPE_KEY, GRANT_TYPE_VALUE_AUTH_KEY);
            form.put(CLIENT_ID_KEY, appleProperties.getClientId());
            form.put(CLIENT_SECRET_KEY, generateClientSecret());
            form.put(AUTH_CODE_KEY, authCode);
            return appleAuthApiRepository.generateAndValidationToken(form);
        } catch (FeignException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ViewithException(ViewithErrorCode.INVALID_OAUTH_TOKEN);
        }
    }


    public UpdateTokenResDto updateAccessToken(String refreshToken) {
        try {
            Map<String, String> form = new HashMap<>();
            form.put(GRANT_TYPE_KEY, GRANT_TYPE_VALUE_REFRESH_TOKEN);
            form.put(CLIENT_ID_KEY, appleProperties.getClientId());
            form.put(CLIENT_SECRET_KEY, generateClientSecret());
            form.put(REFRESH_TOKEN_KEY, refreshToken);
            return appleAuthApiRepository.generateAndValidationToken(form);
        } catch (FeignException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ViewithException(ViewithErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public void revoke(String refreshToken) {
        try {
            Map<String, String> form = new HashMap<>();
            form.put(CLIENT_ID_KEY, appleProperties.getClientId());
            form.put(CLIENT_SECRET_KEY, generateClientSecret());
            form.put(REFRESH_TOKEN_KEY, refreshToken);
            form.put(TOKEN_TYPE_HINT_KEY, GRANT_TYPE_VALUE_REFRESH_TOKEN);
            appleAuthApiRepository.revoke(form);
        } catch (FeignException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ViewithException(ViewithErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public String validateAppleOAuthAndGetSub(String idToken) {
        try {
            Claims claims = JwtUtil.getClaimsBy(getAppleJwtKey(), idToken);
            return claims.getSubject(); // Apple OAuth ID (sub)
        } catch (Exception e) {
            throw new ViewithException(ViewithErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    private AppleJwtKeyDto getAppleJwtKey() {
        return appleAuthApiRepository.getKey();
    }

    private String generateClientSecret() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        LocalDateTime expiration = LocalDateTime.now().plusMonths(5);

        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, appleProperties.getLoginKey())
                .setIssuer(appleProperties.getTeamId())
                .setAudience(appleProperties.getAudience())
                .setSubject(appleProperties.getClientId())
                .setExpiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                .setIssuedAt(new Date())
                .signWith(appleOAuthPrivateKey, SignatureAlgorithm.ES256)
                .compact();
    }


}

