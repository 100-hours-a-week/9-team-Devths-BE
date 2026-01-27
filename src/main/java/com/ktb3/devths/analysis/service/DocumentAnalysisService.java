package com.ktb3.devths.analysis.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.analysis.dto.request.DocumentAnalysisRequest;
import com.ktb3.devths.analysis.dto.request.FastApiAnalysisRequest;
import com.ktb3.devths.analysis.dto.response.DocumentAnalysisResponse;
import com.ktb3.devths.analysis.dto.response.FastApiAnalysisResponse;
import com.ktb3.devths.analysis.dto.response.FastApiTaskStatusResponse;
import com.ktb3.devths.chatbot.domain.entity.AiChatMessage;
import com.ktb3.devths.chatbot.domain.entity.AiChatRoom;
import com.ktb3.devths.chatbot.repository.AiChatRoomRepository;
import com.ktb3.devths.chatbot.service.AiChatMessageService;
import com.ktb3.devths.global.async.domain.constant.TaskStatus;
import com.ktb3.devths.global.async.domain.constant.TaskType;
import com.ktb3.devths.global.async.domain.entity.AsyncTask;
import com.ktb3.devths.global.async.repository.AsyncTaskRepository;
import com.ktb3.devths.global.async.service.AsyncTaskService;
import com.ktb3.devths.global.config.properties.FastApiProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.storage.domain.entity.S3Attachment;
import com.ktb3.devths.global.storage.repository.S3AttachmentRepository;
import com.ktb3.devths.global.util.LogSanitizer;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {

	private final UserRepository userRepository;
	private final AiChatRoomRepository aiChatRoomRepository;
	private final S3AttachmentRepository s3AttachmentRepository;
	private final AsyncTaskRepository asyncTaskRepository;
	private final AsyncTaskService asyncTaskService;
	private final FastApiClient fastApiClient;
	private final FastApiProperties fastApiProperties;
	private final AiChatMessageService aiChatMessageService;

	@Transactional
	public DocumentAnalysisResponse startAnalysis(Long userId, Long roomId,
		DocumentAnalysisRequest request) {

		User user = userRepository.findByIdAndIsWithdrawFalse(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		AiChatRoom chatRoom = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.AI_CHATROOM_NOT_FOUND));

		if (!chatRoom.getUser().getId().equals(userId)) {
			throw new CustomException(ErrorCode.AI_CHATROOM_ACCESS_DENIED);
		}

		validateDocumentInfo(request.resume());
		validateDocumentInfo(request.jobPosting());

		List<AsyncTask> existingTasks = asyncTaskRepository.findByReferenceIdAndTaskTypeAndStatusIn(
			roomId,
			TaskType.ANALYSIS,
			List.of(TaskStatus.PENDING, TaskStatus.PROCESSING)
		);

		if (!existingTasks.isEmpty()) {
			AsyncTask existingTask = existingTasks.get(0);
			log.info("이미 진행 중인 분석 작업 존재: taskId={}", existingTask.getId());
			return new DocumentAnalysisResponse(
				existingTask.getId(),
				existingTask.getStatus().name()
			);
		}

		AsyncTask task = asyncTaskService.createTask(user, TaskType.ANALYSIS, roomId);

		processAnalysis(task.getId(), userId, roomId, request);

		return new DocumentAnalysisResponse(task.getId(), TaskStatus.PENDING.name());
	}

	private void validateDocumentInfo(DocumentAnalysisRequest.DocumentInfo documentInfo) {
		if (!documentInfo.hasFileReference() && (documentInfo.text() == null || documentInfo.text()
			.isBlank())) {
			throw new CustomException(ErrorCode.INVALID_FILE_REFERENCE);
		}
	}

	@Async("taskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void processAnalysis(Long taskId, Long userId, Long roomId, DocumentAnalysisRequest request) {
		try {
			log.info("비동기 분석 처리 시작: taskId={}, roomId={}", taskId, roomId);

			asyncTaskService.updateStatus(taskId, TaskStatus.PROCESSING);

			FastApiAnalysisRequest fastApiRequest = buildFastApiRequest(userId, roomId, request);

			FastApiAnalysisResponse analysisResponse = fastApiClient.requestAnalysis(fastApiRequest);

			asyncTaskService.setExternalTaskId(taskId, analysisResponse.taskId());

			FastApiTaskStatusResponse statusResponse = pollFastApiTask(analysisResponse.taskId());

			if ("completed".equalsIgnoreCase(statusResponse.status())) {
				handleAnalysisSuccess(taskId, roomId, statusResponse);
			} else {
				handleAnalysisFailure(taskId, "FastAPI에서 분석이 완료되지 않았습니다: "
					+ LogSanitizer.sanitize(statusResponse.status()));
			}

		} catch (CustomException e) {
			log.error("분석 처리 중 오류 발생: taskId={}", taskId, e);
			handleAnalysisFailure(taskId, e.getErrorCode().getMessage());
		} catch (Exception e) {
			log.error("분석 처리 중 예상치 못한 오류 발생: taskId={}", taskId, e);
			handleAnalysisFailure(taskId, "분석 처리 중 오류가 발생했습니다");
		}
	}

	private FastApiAnalysisRequest buildFastApiRequest(Long userId, Long roomId,
		DocumentAnalysisRequest request) {

		FastApiAnalysisRequest.FastApiDocumentInfo resumeInfo = buildDocumentInfo(request.resume(), userId);
		FastApiAnalysisRequest.FastApiDocumentInfo jobPostingInfo = buildDocumentInfo(request.jobPosting(), userId);

		return new FastApiAnalysisRequest(
			request.model().name().toLowerCase(),
			roomId,
			userId,
			resumeInfo,
			jobPostingInfo
		);
	}

	private FastApiAnalysisRequest.FastApiDocumentInfo buildDocumentInfo(
		DocumentAnalysisRequest.DocumentInfo documentInfo, Long userId) {

		String s3Key = documentInfo.s3Key();
		String fileType = documentInfo.fileType();
		Long fileId = documentInfo.fileId();

		if (fileId != null) {
			S3Attachment attachment = s3AttachmentRepository.findById(fileId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_FILE_REFERENCE));

			if (!attachment.getUser().getId().equals(userId)) {
				throw new CustomException(ErrorCode.ACCESS_DENIED);
			}

			s3Key = attachment.getS3Key();
			fileType = attachment.getMimeType();
		}

		return new FastApiAnalysisRequest.FastApiDocumentInfo(
			fileId,
			s3Key,
			fileType,
			documentInfo.text()
		);
	}

	private FastApiTaskStatusResponse pollFastApiTask(String fastApiTaskId) {
		int maxAttempts = fastApiProperties.getMaxPollAttempts();
		int pollInterval = fastApiProperties.getPollInterval();

		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			try {
				FastApiTaskStatusResponse response = fastApiClient.pollTaskStatus(fastApiTaskId);

				if ("completed".equalsIgnoreCase(response.status())) {
					log.info("FastAPI 작업 완료: taskId={}", LogSanitizer.sanitize(fastApiTaskId));
					return response;
				} else if ("failed".equalsIgnoreCase(response.status())) {
					throw new CustomException(ErrorCode.ANALYSIS_FAILED);
				}

				Thread.sleep(pollInterval);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CustomException(ErrorCode.ANALYSIS_FAILED);
			}
		}

		throw new CustomException(ErrorCode.FASTAPI_TIMEOUT);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected void handleAnalysisSuccess(Long taskId, Long roomId, FastApiTaskStatusResponse statusResponse) {
		try {
			AiChatRoom chatRoom = aiChatRoomRepository.findByIdAndIsDeletedFalse(roomId)
				.orElse(null);

			if (chatRoom == null) {
				log.warn("채팅방이 삭제되었습니다. 메시지 저장을 건너뜁니다: roomId={}", roomId);
				Map<String, Object> result = new HashMap<>();
				result.put("fastApiTaskId", statusResponse.taskId());
				result.put("summary", "채팅방이 삭제되어 결과를 저장할 수 없습니다");
				asyncTaskService.updateResult(taskId, result);
				return;
			}

			String summary = extractSummary(statusResponse.result());

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("summary", summary);
			metadata.put("taskId", taskId);
			metadata.put("analysisType", "DOCUMENT_ANALYSIS");

			AiChatMessage message = aiChatMessageService.saveReportMessage(
				roomId,
				formatAnalysisResult(statusResponse.result()),
				metadata
			);

			chatRoom.updateTitle(summary);

			Map<String, Object> result = new HashMap<>();
			result.put("messageId", message.getId());
			result.put("fastApiTaskId", statusResponse.taskId());
			result.put("summary", summary);
			result.put("analysisData", statusResponse.result());

			asyncTaskService.updateResult(taskId, result);

			log.info("분석 완료 및 결과 저장 성공: taskId={}, messageId={}", taskId, message.getId());

		} catch (Exception e) {
			log.error("분석 성공 처리 중 오류 발생: taskId={}", taskId, e);
			throw e;
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected void handleAnalysisFailure(Long taskId, String reason) {
		asyncTaskService.markAsFailed(taskId, reason);
		log.error("분석 실패 처리 완료: taskId={}, reason={}", taskId, LogSanitizer.sanitize(reason));
	}

	private String extractSummary(Map<String, Object> result) {
		if (result == null) {
			return "이력서 및 포트폴리오 분석 결과";
		}

		Object resumeAnalysis = result.get("resume_analysis");
		if (resumeAnalysis instanceof Map) {
			Object summary = ((Map<?, ?>) resumeAnalysis).get("summary");
			if (summary != null) {
				return summary.toString();
			}
		}

		return "이력서 및 포트폴리오 분석 결과";
	}

	private String formatAnalysisResult(Map<String, Object> result) {
		if (result == null) {
			return "분석 결과를 생성할 수 없습니다.";
		}

		StringBuilder content = new StringBuilder();
		content.append("## 이력서 및 포트폴리오 분석 결과\n\n");

		Object resumeAnalysis = result.get("resume_analysis");
		if (resumeAnalysis instanceof Map) {
			content.append("### 이력서 분석\n");
			Map<?, ?> analysis = (Map<?, ?>)resumeAnalysis;

			if (analysis.containsKey("strengths")) {
				content.append("\n**강점:**\n");
				appendList(content, analysis.get("strengths"));
			}

			if (analysis.containsKey("weaknesses")) {
				content.append("\n**개선점:**\n");
				appendList(content, analysis.get("weaknesses"));
			}

			if (analysis.containsKey("suggestions")) {
				content.append("\n**제안사항:**\n");
				appendList(content, analysis.get("suggestions"));
			}
		}

		Object postingAnalysis = result.get("posting_analysis");
		if (postingAnalysis instanceof Map) {
			content.append("\n### 채용공고 분석\n");
			Map<?, ?> analysis = (Map<?, ?>)postingAnalysis;

			if (analysis.containsKey("company")) {
				content.append("\n**기업:** ").append(analysis.get("company")).append("\n");
			}

			if (analysis.containsKey("position")) {
				content.append("**포지션:** ").append(analysis.get("position")).append("\n");
			}

			if (analysis.containsKey("required_skills")) {
				content.append("\n**필수 기술:**\n");
				appendList(content, analysis.get("required_skills"));
			}

			if (analysis.containsKey("preferred_skills")) {
				content.append("\n**우대 기술:**\n");
				appendList(content, analysis.get("preferred_skills"));
			}
		}

		return content.toString();
	}

	private void appendList(StringBuilder content, Object listObj) {
		if (listObj instanceof List) {
			List<?> list = (List<?>)listObj;
			for (Object item : list) {
				content.append("- ").append(item).append("\n");
			}
		}
	}
}
