package com.ktb3.devths.vulnerable.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * INTENTIONALLY VULNERABLE ACCESS CONTROL SERVICE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Service
public class VulnerableAccessService {

    // 취약점: IDOR - 사용자 정보 조회
    public Map<String, Object> getUserInfo(Long userId) {
        // 권한 체크 없이 모든 사용자 정보 반환
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("username", "user" + userId);
        user.put("email", "user" + userId + "@example.com");
        user.put("ssn", "123-45-6789"); // 민감한 정보
        user.put("creditCard", "4532-1234-5678-9012"); // 민감한 정보
        return user;
    }

    // 취약점: IDOR - 계좌 정보 조회
    public Map<String, Object> getAccountInfo(String accountId) {
        Map<String, Object> account = new HashMap<>();
        account.put("accountId", accountId);
        account.put("balance", 10000.50);
        account.put("accountNumber", "1234567890");
        return account;
    }

    // 취약점: IDOR - 문서 삭제
    public String deleteDocument(Long documentId) {
        // 소유자 확인 없이 삭제
        return "Document " + documentId + " deleted";
    }

    // 취약점: 권한 체크 없이 모든 사용자 조회
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("users", "All users data...");
        return result;
    }

    // 취약점: 권한 체크 없이 관리자 승격
    public String promoteUserToAdmin(Long userId) {
        // 권한 체크 없음
        return "User " + userId + " promoted to admin";
    }

    // 취약점: 다른 사용자 프로필 수정
    public String updateUserProfile(Long userId, Map<String, Object> profile) {
        // 현재 사용자가 해당 userId인지 확인하지 않음
        return "Profile updated for user " + userId;
    }

    // 취약점: Mass Assignment
    public String registerUser(Map<String, Object> userData) {
        // isAdmin, role 등도 설정 가능
        String username = (String) userData.get("username");
        Boolean isAdmin = (Boolean) userData.getOrDefault("isAdmin", false);
        String role = (String) userData.getOrDefault("role", "USER");

        return "User registered: " + username + ", isAdmin: " + isAdmin + ", role: " + role;
    }

    // 취약점: 순차적 리소스 접근
    public Map<String, Object> getInvoice(int invoiceNumber) {
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("amount", 1234.56);
        invoice.put("customerName", "Customer " + invoiceNumber);
        return invoice;
    }

    // 취약점: 파일 접근 권한 체크 없음
    public String getFileContent(String fileId) {
        return "Content of file " + fileId;
    }

    // 취약점: API 키 노출
    public Map<String, String> getUserApiKey(Long userId) {
        Map<String, String> result = new HashMap<>();
        result.put("apiKey", "sk-user" + userId + "-key123456");
        return result;
    }

    // 취약점: 세션 재생성 없는 로그인
    public String loginWithoutSessionRegeneration(String username, String password) {
        // 세션 고정 공격에 취약
        return "Logged in as " + username + " (session not regenerated)";
    }

    // 취약점: 토큰 검증 없이 비밀번호 재설정
    public String resetPasswordNoVerification(String email, String newPassword) {
        return "Password reset for " + email;
    }

    // 취약점: 사용자명 존재 여부 노출
    public boolean usernameExists(String username) {
        // 실제로는 DB 조회
        return username.equals("admin") || username.equals("user");
    }

    // 취약점: 비밀번호 확인
    public boolean checkPassword(String username, String password) {
        // 간단한 예시
        return "password123".equals(password);
    }

    // 취약점: Race Condition
    public String purchaseItemWithRaceCondition(Long userId, Long itemId) {
        // 잔액 확인
        double balance = getUserBalance(userId);
        double itemPrice = getItemPrice(itemId);

        if (balance >= itemPrice) {
            // Race condition: 여기서 다른 스레드가 잔액을 변경할 수 있음
            try {
                Thread.sleep(100); // 의도적으로 지연
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 잔액 차감
            updateUserBalance(userId, balance - itemPrice);
            return "Purchase successful";
        }

        return "Insufficient balance";
    }

    private double getUserBalance(Long userId) {
        return 1000.0;
    }

    private double getItemPrice(Long itemId) {
        return 100.0;
    }

    private void updateUserBalance(Long userId, double newBalance) {
        // 잔액 업데이트
    }

    // 취약점: 보호된 데이터 노출
    public Map<String, Object> getProtectedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("secret", "This is sensitive data");
        data.put("apiKey", "sk-secret-key-123");
        return data;
    }

    // 취약점: 민감한 데이터 노출
    public Map<String, Object> getSensitiveData() {
        Map<String, Object> data = new HashMap<>();
        data.put("ssn", "123-45-6789");
        data.put("creditCard", "4532-1234-5678-9012");
        return data;
    }

    // 취약점: Rate limiting 없음
    public String sendOtpNoRateLimit(String phoneNumber) {
        // OTP 생성 및 발송
        String otp = String.valueOf((int) (Math.random() * 999999));
        return "OTP sent to " + phoneNumber + ": " + otp;
    }
}
