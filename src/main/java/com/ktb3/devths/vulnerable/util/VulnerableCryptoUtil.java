package com.ktb3.devths.vulnerable.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * INTENTIONALLY VULNERABLE CRYPTOGRAPHY UTILITIES FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Component
public class VulnerableCryptoUtil {

	// 취약점 #1: 하드코딩된 암호화 키
	private static final String DES_KEY = "12345678";
	private static final String AES_KEY = "1234567890123456"; // 128-bit
	private static final byte[] IV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	// 취약점 #2: DES 암호화 (약한 알고리즘)
	public String encryptDES(String data) throws Exception {
		DESKeySpec keySpec = new DESKeySpec(DES_KEY.getBytes());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey key = keyFactory.generateSecret(keySpec);

		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// 취약점 #3: ECB 모드 사용 (패턴 노출)
	public String encryptAES_ECB(String data) throws Exception {
		SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// 취약점 #4: 고정된 IV 사용
	public String encryptAES_CBC_FixedIV(String data) throws Exception {
		SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(IV);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// 취약점 #5: MD5 해시 사용
	public String hashMD5(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] hash = md.digest(data.getBytes());
		return Base64.getEncoder().encodeToString(hash);
	}

	// 취약점 #6: SHA-1 해시 사용
	public String hashSHA1(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] hash = md.digest(data.getBytes());
		return Base64.getEncoder().encodeToString(hash);
	}

	// 취약점 #7: Salt 없이 해시
	public String hashPasswordNoSalt(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] hash = md.digest(password.getBytes());
		return Base64.getEncoder().encodeToString(hash);
	}

	// 취약점 #8: 약한 키 크기의 RSA (512-bit)
	public KeyPair generateWeakRSAKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(512); // 너무 작은 키 크기
		return keyGen.generateKeyPair();
	}

	// 취약점 #9: 패딩 없는 RSA
	public String encryptRSA_NoPadding(String data, PublicKey publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// 취약점 #10: 예측 가능한 난수 생성기
	public byte[] generateWeakRandomKey(int length) {
		Random random = new Random(System.currentTimeMillis());
		byte[] key = new byte[length];
		random.nextBytes(key);
		return key;
	}

	// 취약점 #11: 시드 고정된 난수 생성기
	public int generatePredictableRandom() {
		Random random = new Random(12345L); // 고정된 시드
		return random.nextInt();
	}

	// 취약점 #12: XOR "암호화"
	public String xorEncrypt(String data, String key) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			result.append((char)(data.charAt(i) ^ key.charAt(i % key.length())));
		}
		return Base64.getEncoder().encodeToString(result.toString().getBytes());
	}

	// 취약점 #13: Caesar Cipher (고전 암호)
	public String caesarCipher(String data, int shift) {
		StringBuilder result = new StringBuilder();
		for (char c : data.toCharArray()) {
			if (Character.isLetter(c)) {
				char base = Character.isUpperCase(c) ? 'A' : 'a';
				result.append((char)((c - base + shift) % 26 + base));
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	// 취약점 #14: 키를 파일에 저장 (평문)
	public void saveKeyToFile(SecretKey key, String filename) throws Exception {
		java.io.FileWriter writer = new java.io.FileWriter(filename);
		writer.write(Base64.getEncoder().encodeToString(key.getEncoded()));
		writer.close();
	}

	// 취약점 #15: 약한 PBE (Password-Based Encryption)
	public String encryptPBE_Weak(String data, String password) throws Exception {
		// 낮은 iteration count
		int iterationCount = 10; // 너무 작음 (최소 10000 권장)

		byte[] salt = {1, 2, 3, 4, 5, 6, 7, 8}; // 고정된 salt

		javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec(
			password.toCharArray(),
			salt,
			iterationCount,
			128
		);

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory.generateSecret(keySpec);

		Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
		cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.PBEParameterSpec(salt, iterationCount));

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// 취약점 #16: Null Cipher
	public String nullCipher(String data) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		byte[] keyBytes = new byte[16]; // 모두 0인 키
		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		// 패딩을 맞추기 위해 길이 조정
		byte[] paddedData = new byte[16];
		System.arraycopy(data.getBytes(), 0, paddedData, 0, Math.min(data.length(), 16));

		return Base64.getEncoder().encodeToString(cipher.doFinal(paddedData));
	}

	// 취약점 #17: 서명 검증 누락
	public boolean verifySignatureVulnerable(byte[] data, byte[] signature, PublicKey publicKey) {
		// 취약점: 실제 검증을 하지 않고 항상 true 반환
		return true;
	}

	// 취약점 #18: 인증서 검증 비활성화
	public void disableCertificateValidation() throws Exception {
		// 취약점: SSL/TLS 인증서 검증 비활성화
		javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
			new javax.net.ssl.X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			}
		};

		javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	// 취약점 #19: 호스트명 검증 비활성화
	public void disableHostnameVerification() {
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
			(hostname, session) -> true
		);
	}

	// 취약점 #20: 약한 TLS 버전 사용
	public javax.net.ssl.SSLContext createWeakSSLContext() throws Exception {
		// TLS 1.0 사용 (취약)
		return javax.net.ssl.SSLContext.getInstance("TLSv1");
	}
}
