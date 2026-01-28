package com.ktb3.devths.ai.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb3.devths.ai.analysis.dto.request.FastApiAnalysisRequest;
import com.ktb3.devths.ai.analysis.dto.response.FastApiAnalysisResponse;
import com.ktb3.devths.ai.analysis.dto.response.FastApiTaskStatusResponse;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiChatRequest;
import com.ktb3.devths.ai.chatbot.dto.request.FastApiInterviewEvaluationRequest;
import com.ktb3.devths.global.config.properties.FastApiProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.util.LogSanitizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiClient {

	private final RestClient restClient;
	private final WebClient webClient;
	private final FastApiProperties fastApiProperties;
	private final ObjectMapper objectMapper;

	public FastApiAnalysisResponse requestAnalysis(FastApiAnalysisRequest request) {
		try {
			String url = fastApiProperties.getBaseUrl() + "/ai/text/extract";

			log.info("FastAPI 분석 요청 전송: taskId={}, roomId={}, userId={}",
				request.taskId(), request.roomId(), request.userId());

			FastApiAnalysisResponse response = restClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(FastApiAnalysisResponse.class);

			if (response == null) {
				throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
			}

			log.info("FastAPI 분석 요청 성공 - 요청 taskId={}, 응답 taskId={}",
				request.taskId(), response.taskId());
			return response;

		} catch (RestClientException e) {
			log.error("FastAPI 분석 요청 실패", e);
			throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
		}
	}

	public FastApiTaskStatusResponse pollTaskStatus(Long taskId) {
		try {
			String url = fastApiProperties.getBaseUrl() + "/ai/task/" + taskId;

			FastApiTaskStatusResponse response = restClient.get()
				.uri(url)
				.retrieve()
				.body(FastApiTaskStatusResponse.class);

			if (response == null) {
				throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
			}

			return response;

		} catch (RestClientException e) {
			log.error("FastAPI 작업 상태 조회 실패: taskId={}", taskId, e);
			throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
		}
	}

	public Flux<String> streamChatResponse(FastApiChatRequest request) {
		String url = fastApiProperties.getBaseUrl() + "/ai/chat";
		return webClient.post()
			.uri(url)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.bodyValue(request)
			.retrieve()
			.bodyToFlux(String.class)
			.map(this::parseChunk)
			.filter(chunk -> !chunk.equals("[DONE]"))
			.doOnError(e -> log.error("FastAPI 스트리밍 실패", e))
			.onErrorResume(e -> {
				log.error("FastAPI 스트리밍 에러", e);
				return Flux.error(new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED));
			});
	}

	public Flux<String> streamInterviewEvaluation(FastApiInterviewEvaluationRequest request) {
		String url = fastApiProperties.getBaseUrl() + "/ai/chat";
		return webClient.post()
			.uri(url)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.TEXT_EVENT_STREAM)
			.bodyValue(request)
			.retrieve()
			.bodyToFlux(String.class)
			.map(this::parseChunk)
			.filter(chunk -> !chunk.equals("[DONE]"))
			.doOnError(e -> log.error("FastAPI 면접 평가 스트리밍 실패", e))
			.onErrorResume(e -> {
				log.error("FastAPI 면접 평가 스트리밍 에러", e);
				return Flux.error(new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED));
			});
	}

	private String parseChunk(String sseData) {
		try {
			if (sseData.startsWith("data: ")) {
				String jsonData = sseData.substring(6).trim();

				if (jsonData.equals("[DONE]")) {
					return "[DONE]";
				}

				JsonNode node = objectMapper.readTree(jsonData);
				if (node.has("chunk")) {
					return node.get("chunk").asText();
				}

				return "";
			}
			return "";
		} catch (Exception e) {
			log.error("청크 파싱 실패: data={}", LogSanitizer.sanitize(sseData), e);
			return "";
		}
	}
}
