package com.ktb3.devths.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.user.domain.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByIdAndIsWithdrawFalse(Long id);

	boolean existsByEmail(String email);

	boolean existsByNickname(String nickname);
}
