package com.ktb3.devths.global.ratelimit.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

	private GoogleCalendarLimit googleCalendar = new GoogleCalendarLimit();
	private GoogleTasksLimit googleTasks = new GoogleTasksLimit();
	private FastApiLimit fastapi = new FastApiLimit();
	private GoogleOAuthLimit googleOauth = new GoogleOAuthLimit();

	@Getter
	@Setter
	public static class GoogleCalendarLimit {
		private int bucketCapacity = 100;
		private boolean enabled = true;
	}

	@Getter
	@Setter
	public static class GoogleTasksLimit {
		private int bucketCapacity = 100;
		private boolean enabled = true;
	}

	@Getter
	@Setter
	public static class FastApiLimit {
		private int bucketCapacity = 10;
		private boolean enabled = true;
	}

	@Getter
	@Setter
	public static class GoogleOAuthLimit {
		private int bucketCapacity = 50;
		private boolean enabled = true;
	}
}
