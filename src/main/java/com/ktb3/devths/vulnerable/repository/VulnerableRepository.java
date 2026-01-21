package com.ktb3.devths.vulnerable.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * INTENTIONALLY VULNERABLE CODE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Repository
@RequiredArgsConstructor
public class VulnerableRepository {

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	// SQL Injection 취약점 #1 - JdbcTemplate의 잘못된 사용
	public List<Map<String, Object>> findUsersByName(String name) {
		// 취약점: 쿼리 문자열에 직접 연결
		String sql = "SELECT * FROM users WHERE name LIKE '%" + name + "%'";
		return jdbcTemplate.queryForList(sql);
	}

	// SQL Injection 취약점 #2 - ORDER BY 절
	public List<Map<String, Object>> findUsersOrderBy(String orderBy) {
		// 취약점: ORDER BY 절에 사용자 입력 직접 사용
		String sql = "SELECT * FROM users ORDER BY " + orderBy;
		return jdbcTemplate.queryForList(sql);
	}

	// SQL Injection 취약점 #3 - IN 절
	public List<Map<String, Object>> findUsersByIds(String ids) {
		// 취약점: IN 절에 사용자 입력 직접 사용
		String sql = "SELECT * FROM users WHERE id IN (" + ids + ")";
		return jdbcTemplate.queryForList(sql);
	}

	// SQL Injection 취약점 #4 - UPDATE 문
	public void updateUserEmail(String userId, String email) {
		// 취약점: UPDATE 문에 사용자 입력 직접 사용
		String sql = "UPDATE users SET email = '" + email + "' WHERE id = " + userId;
		jdbcTemplate.update(sql);
	}

	// SQL Injection 취약점 #5 - DELETE 문
	public void deleteUserByUsername(String username) {
		// 취약점: DELETE 문에 사용자 입력 직접 사용
		String sql = "DELETE FROM users WHERE username = '" + username + "'";
		jdbcTemplate.update(sql);
	}

	// SQL Injection 취약점 #6 - 복잡한 쿼리
	public List<Map<String, Object>> searchWithFilters(String name, String role, String department) {
		// 취약점: 여러 필터를 동적으로 연결
		StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");

		if (name != null) {
			sql.append(" AND name = '").append(name).append("'");
		}
		if (role != null) {
			sql.append(" AND role = '").append(role).append("'");
		}
		if (department != null) {
			sql.append(" AND department = '").append(department).append("'");
		}

		return jdbcTemplate.queryForList(sql.toString());
	}

	// SQL Injection 취약점 #7 - LIMIT 절
	public List<Map<String, Object>> findUsersWithLimit(String limit) {
		// 취약점: LIMIT 절에 사용자 입력 직접 사용
		String sql = "SELECT * FROM users LIMIT " + limit;
		return jdbcTemplate.queryForList(sql);
	}

	// SQL Injection 취약점 #8 - Native SQL with Statement
	public List<Map<String, Object>> executeRawQuery(String tableName, String columnName, String value) {
		List<Map<String, Object>> results = new ArrayList<>();

		try (Connection conn = dataSource.getConnection()) {
			// 취약점: 테이블명, 컬럼명, 값 모두 사용자 입력으로 구성
			String query = "SELECT * FROM " + tableName +
				" WHERE " + columnName + " = '" + value + "'";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				Map<String, Object> row = new HashMap<>();
				int columnCount = rs.getMetaData().getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
				}
				results.add(row);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Query execution failed", e);
		}

		return results;
	}

	// SQL Injection 취약점 #9 - Batch operations
	public void batchUpdateUsers(List<String> userIds, String status) {
		// 취약점: 배치 작업에서 사용자 입력 직접 사용
		for (String userId : userIds) {
			String sql = "UPDATE users SET status = '" + status + "' WHERE id = " + userId;
			jdbcTemplate.update(sql);
		}
	}

	// SQL Injection 취약점 #10 - JOIN with user input
	public List<Map<String, Object>> findUsersWithJoin(String joinTable, String condition) {
		// 취약점: JOIN 절과 조건에 사용자 입력 직접 사용
		String sql = "SELECT u.* FROM users u JOIN " + joinTable +
			" ON " + condition;
		return jdbcTemplate.queryForList(sql);
	}

	// NoSQL Injection 취약점 (MongoDB 예시, 개념적)
	public String buildMongoQuery(String username, String role) {
		// 취약점: NoSQL 쿼리 문자열 직접 구성
		return "{ username: '" + username + "', role: '" + role + "' }";
	}

	// Blind SQL Injection 취약점
	public boolean checkUserExists(String username) {
		try {
			String sql = "SELECT COUNT(*) FROM users WHERE username = '" + username + "'";
			Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
			return count != null && count > 0;
		} catch (Exception e) {
			// 취약점: 에러 정보 노출
			throw new RuntimeException("Error checking user: " + e.getMessage());
		}
	}

	// Second-Order SQL Injection 취약점
	public void saveUserInput(String userId, String input) {
		// 취약점: 저장 시에는 안전하지만, 나중에 사용할 때 취약
		String sql = "INSERT INTO user_inputs (user_id, input) VALUES (" + userId + ", '" + input + "')";
		jdbcTemplate.update(sql);
	}

	public List<Map<String, Object>> retrieveAndUseUserInput(String userId) {
		// 취약점: 저장된 값을 검증 없이 쿼리에 사용
		String inputSql = "SELECT input FROM user_inputs WHERE user_id = " + userId;
		List<String> inputs = jdbcTemplate.queryForList(inputSql, String.class);

		List<Map<String, Object>> results = new ArrayList<>();
		for (String input : inputs) {
			String sql = "SELECT * FROM data WHERE value = '" + input + "'";
			results.addAll(jdbcTemplate.queryForList(sql));
		}

		return results;
	}
}
