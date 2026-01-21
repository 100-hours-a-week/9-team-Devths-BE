package com.ktb3.devths.vulnerable.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ktb3.devths.vulnerable.service.VulnerableService;

import lombok.RequiredArgsConstructor;

/**
 * INTENTIONALLY VULNERABLE CODE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@RestController
@RequestMapping("/api/vulnerable")
@RequiredArgsConstructor
public class VulnerableController {

	private final VulnerableService vulnerableService;

	// SQL Injection 취약점 #1 - 사용자 입력을 직접 쿼리에 포함
	@GetMapping("/sql-injection/search")
	public List<Map<String, Object>> searchUser(@RequestParam String username) {
		return vulnerableService.searchUserVulnerable(username);
	}

	// SQL Injection 취약점 #2 - 로그인 우회 가능
	@PostMapping("/sql-injection/login")
	public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
		return vulnerableService.loginVulnerable(username, password);
	}

	// XSS 취약점 #1 - 사용자 입력을 그대로 반환 (Reflected XSS)
	@GetMapping("/xss/reflect")
	public String reflectInput(@RequestParam String input) {
		return "<html><body><h1>Your input: " + input + "</h1></body></html>";
	}

	// XSS 취약점 #2 - 저장된 XSS
	@PostMapping("/xss/store")
	public String storeComment(@RequestParam String comment) {
		return vulnerableService.storeCommentVulnerable(comment);
	}

	// Command Injection 취약점 #1 - ping 명령어
	@GetMapping("/command-injection/ping")
	public String pingHost(@RequestParam String host) throws IOException {
		return vulnerableService.pingHostVulnerable(host);
	}

	// Command Injection 취약점 #2 - 파일 목록 조회
	@GetMapping("/command-injection/list")
	public String listFiles(@RequestParam String directory) throws IOException {
		return vulnerableService.listFilesVulnerable(directory);
	}

	// Path Traversal 취약점 #1 - 파일 읽기
	@GetMapping("/path-traversal/read")
	public String readFile(@RequestParam String filename) throws IOException {
		return vulnerableService.readFileVulnerable(filename);
	}

	// Path Traversal 취약점 #2 - 파일 삭제
	@DeleteMapping("/path-traversal/delete")
	public String deleteFile(@RequestParam String filename) {
		return vulnerableService.deleteFileVulnerable(filename);
	}

	// Hard-coded Credentials 취약점
	@GetMapping("/hardcoded/admin-password")
	public String getAdminPassword() {
		return vulnerableService.getHardcodedPassword();
	}

	// Insecure Deserialization 취약점
	@PostMapping("/deserialization")
	public Object deserializeObject(@RequestBody byte[] data) throws Exception {
		return vulnerableService.deserializeVulnerable(data);
	}

	// SSRF (Server-Side Request Forgery) 취약점
	@GetMapping("/ssrf/fetch")
	public String fetchUrl(@RequestParam String url) {
		return vulnerableService.fetchUrlVulnerable(url);
	}

	// Weak Cryptography 취약점
	@PostMapping("/weak-crypto/encrypt")
	public String encryptData(@RequestParam String data) throws Exception {
		return vulnerableService.encryptWithWeakAlgorithm(data);
	}

	// LDAP Injection 취약점
	@GetMapping("/ldap-injection/search")
	public String searchLdap(@RequestParam String username) {
		return vulnerableService.searchLdapVulnerable(username);
	}

	// XXE (XML External Entity) 취약점
	@PostMapping("/xxe/parse")
	public String parseXml(@RequestBody String xml) throws Exception {
		return vulnerableService.parseXmlVulnerable(xml);
	}
}
