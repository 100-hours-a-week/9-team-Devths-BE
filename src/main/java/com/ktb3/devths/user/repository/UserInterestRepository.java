package com.ktb3.devths.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.user.domain.constant.Interests;
import com.ktb3.devths.user.domain.entity.UserInterest;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
	@Query("SELECT ui.interest FROM UserInterest ui WHERE ui.user.id = :userId")
	List<Interests> findInterestsByUserId(@Param("userId") Long userId);

	void deleteAllByUser_Id(Long userId);
}
