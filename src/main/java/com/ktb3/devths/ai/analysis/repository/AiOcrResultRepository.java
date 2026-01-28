package com.ktb3.devths.ai.analysis.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktb3.devths.ai.analysis.domain.AiOcrResult;

public interface AiOcrResultRepository extends JpaRepository<AiOcrResult, Long> {
	Optional<AiOcrResult> findByRoomId(Long roomId);
}
