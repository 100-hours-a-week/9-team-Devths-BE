package com.ktb3.devths.calendar.domain.constant;

public enum InterviewStage {
	DOCUMENT,
	CODING_TEST,
	INTERVIEW;

	private final String displayName;

	InterviewStage() {
		this.displayName = name();
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public static InterviewStage fromDisplayName(String displayName) {
		for (InterviewStage interviewStage : values()) {
			if (interviewStage.displayName.equals(displayName)) {
				return interviewStage;
			}
		}
		return null;
	}
}
