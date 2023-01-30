package net.zhuoweizhang.raspberryjuice;

public enum Logger {
	INSTANCE;
	private static final String STANDARD_PREFIX = "CRYPTLOG [SERVER SIDE] >> ";
	private static final String ERROR_PREFIX = "CRYPTLOG [ERROR] >> ";

	public void log(String message) {
		System.out.println(STANDARD_PREFIX + message);
	}

	public void logError(String error) {
		System.err.println(ERROR_PREFIX + error);
	}
}
