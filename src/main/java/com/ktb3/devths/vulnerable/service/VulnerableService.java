package com.ktb3.devths.vulnerable.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;

import lombok.RequiredArgsConstructor;

/**
 * INTENTIONALLY VULNERABLE CODE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Service
@RequiredArgsConstructor
public class VulnerableService {

	// Hard-coded credentials - 취약점!
	private static final String ADMIN_PASSWORD = "admin123!@#";
	private static final String DB_PASSWORD = "postgresPassword123";
	private static final String API_KEY = "sk-1234567890abcdefghijklmnop";
	private static final String SECRET_TOKEN = "MySecretToken123456789";
	private final DataSource dataSource;

	// SQL Injection 취약점 #1
	public List<Map<String, Object>> searchUserVulnerable(String username) {
		List<Map<String, Object>> results = new ArrayList<>();

		try (Connection conn = dataSource.getConnection()) {
			// 취약점: 사용자 입력을 직접 쿼리에 연결
			String query = "SELECT * FROM users WHERE username = '" + username + "'";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			while (rs.next()) {
				Map<String, Object> row = new HashMap<>();
				for (int i = 1; i <= columnCount; i++) {
					row.put(metaData.getColumnName(i), rs.getObject(i));
				}
				results.add(row);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Database error", e);
		}

		return results;
	}

	// SQL Injection 취약점 #2
	public Map<String, Object> loginVulnerable(String username, String password) {
		Map<String, Object> result = new HashMap<>();

		try (Connection conn = dataSource.getConnection()) {
			// 취약점: SQL Injection으로 인증 우회 가능
			// 예: username = "admin' OR '1'='1", password = "anything"
			String query = "SELECT * FROM users WHERE username = '" + username +
				"' AND password = '" + password + "'";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			if (rs.next()) {
				result.put("success", true);
				result.put("username", rs.getString("username"));
				result.put("role", rs.getString("role"));
			} else {
				result.put("success", false);
			}
		} catch (SQLException e) {
			result.put("error", e.getMessage());
		}

		return result;
	}

	// XSS 취약점 - 저장된 XSS
	public String storeCommentVulnerable(String comment) {
		// 취약점: 사용자 입력을 검증/이스케이프 없이 저장하고 표시
		try (Connection conn = dataSource.getConnection()) {
			String sql = "INSERT INTO comments (content, created_at) VALUES ('" +
				comment + "', NOW())";
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			return "Error: " + e.getMessage();
		}

		return "<html><body><p>Comment stored: " + comment + "</p></body></html>";
	}

	// Command Injection 취약점 #1
	public String pingHostVulnerable(String host) throws IOException {
		// 취약점: 사용자 입력을 검증 없이 시스템 명령어에 사용
		// 예: host = "8.8.8.8; rm -rf /"
		String command = "ping -c 4 " + host;
		Process process = Runtime.getRuntime().exec(command);

		return readProcessOutput(process);
	}

	// Command Injection 취약점 #2
	public String listFilesVulnerable(String directory) throws IOException {
		// 취약점: 사용자 입력을 검증 없이 시스템 명령어에 사용
		String[] command = {"/bin/sh", "-c", "ls -la " + directory};
		Process process = Runtime.getRuntime().exec(command);

		return readProcessOutput(process);
	}

	// Path Traversal 취약점 #1
	public String readFileVulnerable(String filename) throws IOException {
		// 취약점: 경로 검증 없이 파일 읽기
		// 예: filename = "../../etc/passwd"
		File file = new File("/var/app/uploads/" + filename);
		return new String(Files.readAllBytes(file.toPath()));
	}

	// Path Traversal 취약점 #2
	public String deleteFileVulnerable(String filename) {
		// 취약점: 경로 검증 없이 파일 삭제
		File file = new File("/var/app/uploads/" + filename);
		if (file.delete()) {
			return "File deleted: " + filename;
		} else {
			return "Failed to delete: " + filename;
		}
	}

	// Hard-coded Credentials 노출
	public String getHardcodedPassword() {
		// 취약점: 하드코딩된 비밀번호 반환
		return "Admin password: " + ADMIN_PASSWORD +
			", DB password: " + DB_PASSWORD +
			", API Key: " + API_KEY;
	}

	// Insecure Deserialization 취약점
	public Object deserializeVulnerable(byte[] data) throws Exception {
		// 취약점: 검증 없이 역직렬화
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bis);
		return ois.readObject();
	}

	// SSRF 취약점
	public String fetchUrlVulnerable(String url) {
		// 취약점: URL 검증 없이 서버에서 요청
		// 예: url = "http://localhost:8080/admin" 또는 "file:///etc/passwd"
		WebClient client = WebClient.create();
		return client.get()
			.uri(url)
			.retrieve()
			.bodyToMono(String.class)
			.block();
	}

	// Weak Cryptography 취약점
	public String encryptWithWeakAlgorithm(String data) throws Exception {
		// 취약점: 약한 암호화 알고리즘 사용 (DES)
		SecretKeySpec key = new SecretKeySpec("12345678".getBytes(), "DES");
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] encrypted = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// LDAP Injection 취약점
	public String searchLdapVulnerable(String username) {
		// 취약점: LDAP 쿼리에 사용자 입력 직접 사용
		// 예: username = "*)(uid=*))(|(uid=*"
		String filter = "(uid=" + username + ")";

		// 실제 LDAP 연결 코드는 생략하지만 필터 문자열 자체가 취약
		return "Searching LDAP with filter: " + filter;
	}

	// XXE (XML External Entity) 취약점
	public String parseXmlVulnerable(String xml) throws Exception {
		// 취약점: XXE 공격에 취약한 XML 파서
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// 외부 엔티티 처리를 비활성화하지 않음
		DocumentBuilder builder = factory.newDocumentBuilder();

		InputStream is = new ByteArrayInputStream(xml.getBytes());
		Document doc = builder.parse(is);

		return "Parsed: " + doc.getDocumentElement().getTextContent();
	}

	// 추가 취약점: Regex DoS (ReDoS)
	public boolean validateEmailVulnerable(String email) {
		// 취약점: 복잡한 정규식으로 인한 DoS 가능
		String regex = "(a+)+b";
		return email.matches(regex);
	}

	// 추가 취약점: Random number generation with weak seed
	public String generateTokenVulnerable() {
		// 취약점: 예측 가능한 난수 생성
		Random random = new Random(System.currentTimeMillis());
		return String.valueOf(random.nextLong());
	}

	// 추가 취약점: Null cipher
	public String encryptWithNullCipher(String data) throws Exception {
		// 취약점: 실제로 암호화하지 않는 Null Cipher
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		// 잘못된 키 사용
		byte[] keyBytes = new byte[16]; // 모두 0인 키
		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);

		return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
	}

	// Helper method
	private String readProcessOutput(Process process) throws IOException {
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream()));
		StringBuilder output = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			output.append(line).append("\n");
		}

		return output.toString();
	}
}
