package com.ktb3.devths.calendar.domain.constant;

public enum NotificationUnit {
	MINUTE("분"),
	HOUR("시간"),
	DAY("일");

	private final String displayName;

	NotificationUnit(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static NotificationUnit fromDisplayName(String displayName) {
		for (NotificationUnit unit : NotificationUnit.values()) {
			if (unit.getDisplayName().equals(displayName)) {
				return unit;
			}
		}
		return null;
	}
}
