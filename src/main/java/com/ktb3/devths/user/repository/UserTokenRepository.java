package com.ktb3.devths.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.user.domain.entity.UserToken;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
	void deleteByUserId(Long userId);
}