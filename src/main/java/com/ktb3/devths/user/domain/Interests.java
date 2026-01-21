package com.ktb3.devths.user.domain;

public enum Interests {
	BACKEND("백엔드"),
	FRONTEND("프론트엔드"),
	AI("AI"),
	CLOUD("클라우드");

	private final String displayName;

	Interests(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
