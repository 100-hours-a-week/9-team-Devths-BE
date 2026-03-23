package com.ktb3.devths.global.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@Component
@RequiredArgsConstructor
public class TransactionMonitoringAspect {

	private static final String OBSERVATION_NAME = "transaction";

	private final ObservationRegistry observationRegistry;

	@Around("@annotation(org.springframework.transaction.annotation.Transactional)"
		+ " && !within(com.ktb3.devths.chat.service.ChatOutboxEventRelayer)"
		+ " && !within(com.ktb3.devths.chat.service.ChatOutboxRelayService)")
	public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature)joinPoint.getSignature();
		Method method = signature.getMethod();

		Transactional txAnnotation = AnnotationUtils.findAnnotation(method, Transactional.class);

		String className = joinPoint.getTarget().getClass().getSimpleName();
		String methodName = method.getName();
		boolean readOnly = txAnnotation != null && txAnnotation.readOnly();
		String propagation = txAnnotation != null ? txAnnotation.propagation().name() : "REQUIRED";

		Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.lowCardinalityKeyValue("readOnly", String.valueOf(readOnly))
			.lowCardinalityKeyValue("propagation", propagation)
			.highCardinalityKeyValue("code.function", className + "." + methodName)
			.start();

		try {
			return joinPoint.proceed();
		} catch (Throwable e) {
			observation.error(e);
			throw e;
		} finally {
			observation.stop();
		}
	}
}
