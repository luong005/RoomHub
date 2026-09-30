package com.roommate_connection_platform.components;

import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.models.RefreshTokenEntity;
import com.roommate_connection_platform.models.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {
    @Value("${jwt.expiration}")
    private int expiration; //save to an environment variable

    @Value("${jwt.secretKey}")
    private String secretKey;

    public String generateAccessToken(UserEntity user){
        //properties => claims
        Map<String, Object> claims = new HashMap<>();
        //this.generateSecretKey();
        claims.put("phoneNumber", user.getPhoneNumber());
        claims.put("id",user.getId());
        try {
            String token = Jwts.builder()
                    .setClaims(claims) // noi dung them cua payload
                    .setSubject(user.getPhoneNumber()) // xac dinh chu so huu bang sdt (mac dinh)
                    .setExpiration(new Date(System.currentTimeMillis() + 300000L)) //hsd token: 30 ngày
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256) // tao chu ky: header+payload->hash SHA-256
                    .compact();// ket hop thanh token
            return token;
        }catch (Exception e) {
            throw new BusinessException("TOKEN_CREATE_FAILED", "Cannot create jwt token, error: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
    public String generateRefreshToken(UserEntity user){
        //properties => claims
        Map<String, Object> claims = new HashMap<>();
        //this.generateSecretKey();
        claims.put("id",user.getId());
        claims.put("phoneNumber", user.getPhoneNumber());
        try {
            String token = Jwts.builder()
                    .setClaims(claims) // noi dung them cua payload
                    .setSubject(user.getPhoneNumber()) // xac dinh chu so huu bang sdt (mac dinh)
                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L)) //hsd token: 30 ngày
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256) // tao chu ky: header+payload->hash SHA-256
                    .compact();// ket hop thanh token
            return token;
        }catch (Exception e) {
            throw new BusinessException("TOKEN_CREATE_FAILED", "Cannot create jwt token, error: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
    private Key getSigningKey() {
        byte[] bytes = Decoders.BASE64.decode(secretKey); // decode secret key base64->bytes de dung ham signWith
        //Keys.hmacShaKeyFor(Decoders.BASE64.decode("TaqlmGv1iEDMRiFp/pHuID1+T84IABfuA0xXh4GhiUI="));
        return Keys.hmacShaKeyFor(bytes); // tao ra secretkey = algo HMAC
    }

    // Ham giai ma token va lay payload
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // set key cua server
                .build()
                .parseClaimsJws(token) // phan tich xac thuc token
                .getBody(); // lay ra payload
    }

    // Ham lay thong tin tu token
    public  <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //check expiration
    public boolean isTokenExpired(String token) {
        Date expirationDate = this.extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    // Lay sdt
    public String extractPhoneNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ktra token cua user
    public boolean validateToken(String token, UserDetails userDetails) {
        String phoneNumber = extractPhoneNumber(token);
        return (phoneNumber.equals(userDetails.getUsername()) && !isTokenExpired(token)); //check hạn của token
    }

    public RefreshTokenEntity toRefreshTokenEntity(String refreshToken, UserEntity user){
       RefreshTokenEntity entity = new RefreshTokenEntity();
       entity.setUser(user);
       entity.setExpirationDate(extractClaim(refreshToken,Claims::getExpiration));
       entity.setRevoked(true);
       if(isTokenExpired(refreshToken)){
           entity.setRevoked(false);
       }
       return entity;
    }
}

