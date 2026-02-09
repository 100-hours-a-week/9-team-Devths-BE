package com.ktb3.devths.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.board.domain.entity.Like;

public interface LikeRepository extends JpaRepository<Like, Long> {

	boolean existsByPostIdAndUserId(Long postId, Long userId);
}
