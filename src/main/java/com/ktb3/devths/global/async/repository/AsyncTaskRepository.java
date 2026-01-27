package com.ktb3.devths.global.async.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.global.async.domain.constant.TaskStatus;
import com.ktb3.devths.global.async.domain.constant.TaskType;
import com.ktb3.devths.global.async.domain.entity.AsyncTask;

public interface AsyncTaskRepository extends JpaRepository<AsyncTask, Long> {

	@Query("SELECT t FROM AsyncTask t WHERE t.id = :taskId AND t.user.id = :userId")
	Optional<AsyncTask> findByIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

	@Query("SELECT t FROM AsyncTask t WHERE t.referenceId = :referenceId "
		+ "AND t.taskType = :taskType "
		+ "AND t.status IN :statuses "
		+ "ORDER BY t.createdAt DESC")
	List<AsyncTask> findByReferenceIdAndTaskTypeAndStatusIn(
		@Param("referenceId") Long referenceId,
		@Param("taskType") TaskType taskType,
		@Param("statuses") List<TaskStatus> statuses
	);
}
