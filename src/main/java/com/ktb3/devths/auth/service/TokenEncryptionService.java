package com.ktb3.devths.auth.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.ktb3.devths.global.config.properties.EncryptionProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableConfigurationProperties(EncryptionProperties.class)
@RequiredArgsConstructor
public class TokenEncryptionService {
	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;
	private static final int MAX_TOKEN_LENGTH = 10240; // 10KB

	private final EncryptionProperties encryptionProperties;

	/**
	 * Google Access Token / Refresh Token을 AES-256-GCM으로 암호화
	 *
	 * @param plainToken 평문 토큰
	 * @return Base64 인코딩된 암호화 토큰 (IV + 암호문)
	 */
	public String encrypt(String plainToken) {
		if (plainToken == null || plainToken.isEmpty()) {
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
		if (plainToken.length() > MAX_TOKEN_LENGTH) {
			log.error("토큰 암호화 실패: 입력 길이 초과 ({}바이트)", plainToken.length());
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
		try {
			SecretKey key = getSecretKey();

			// IV 생성 (12바이트)
			byte[] iv = new byte[GCM_IV_LENGTH];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);

			// GCM 파라미터 설정
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			// 암호화
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
			byte[] encryptedBytes = cipher.doFinal(plainToken.getBytes());

			// IV + 암호문을 하나의 바이트 배열로 결합
			ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
			byteBuffer.put(iv);
			byteBuffer.put(encryptedBytes);

			// Base64 인코딩
			return Base64.getEncoder().encodeToString(byteBuffer.array());
		} catch (Exception e) {
			log.error("토큰 암호화 실패", e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * AES-256-GCM으로 암호화된 토큰을 복호화
	 *
	 * @param encryptedToken Base64 인코딩된 암호화 토큰 (IV + 암호문)
	 * @return 평문 토큰
	 */
	public String decrypt(String encryptedToken) {
		try {
			SecretKey key = getSecretKey();

			// Base64 디코딩
			byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);

			// IV와 암호문 분리
			ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
			byte[] iv = new byte[GCM_IV_LENGTH];
			byteBuffer.get(iv);
			byte[] encryptedBytes = new byte[byteBuffer.remaining()];
			byteBuffer.get(encryptedBytes);

			// GCM 파라미터 설정
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			// 복호화
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

			return new String(decryptedBytes);
		} catch (Exception e) {
			log.error("토큰 복호화 실패", e);
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Base64 인코딩된 AES 키를 SecretKey로 변환
	 */
	private SecretKey getSecretKey() throws NoSuchAlgorithmException {
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		byte[] keyBytes = sha256.digest(
			encryptionProperties.getAesKey().getBytes(StandardCharsets.UTF_8)
		);
		return new SecretKeySpec(keyBytes, "AES");
	}
}
