package com.ktb3.devths.user.domain.constant;

public enum Interests {
	BACKEND("백엔드"),
	FRONTEND("프론트엔드"),
	AI("인공지능"),
	CLOUD("클라우드");

	private final String displayName;

	Interests(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static Interests fromDisplayName(String displayName) {
		for (Interests interest : values()) {
			if (interest.displayName.equals(displayName)) {
				return interest;
			}
		}
		return null;
	}
}
