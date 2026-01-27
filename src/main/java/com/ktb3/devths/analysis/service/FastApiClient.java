package com.ktb3.devths.analysis.service;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ktb3.devths.analysis.dto.request.FastApiAnalysisRequest;
import com.ktb3.devths.analysis.dto.response.FastApiAnalysisResponse;
import com.ktb3.devths.analysis.dto.response.FastApiTaskStatusResponse;
import com.ktb3.devths.global.config.properties.FastApiProperties;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiClient {

	private final RestClient restClient;
	private final FastApiProperties fastApiProperties;

	public FastApiAnalysisResponse requestAnalysis(FastApiAnalysisRequest request) {
		try {
			String url = fastApiProperties.getBaseUrl() + "/ai/text/extract";

			FastApiAnalysisResponse response = restClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(FastApiAnalysisResponse.class);

			if (response == null) {
				throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
			}

			log.info("FastAPI 분석 요청 성공: taskId={}", response.taskId());
			return response;

		} catch (RestClientException e) {
			log.error("FastAPI 분석 요청 실패", e);
			throw new CustomException(ErrorCode.FASTAPI_CONNECTION_FAILED);
		}
	}

	public FastApiTaskStatusResponse pollTaskStatus(String taskId) {
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
}
