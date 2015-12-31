package Utilities;

/**
 * Created by jared on 12/10/15.
 */
public class StringUtils {
	public static boolean isNullOrWhiteSpace(String value) {
		return value == null || value.trim().isEmpty();
	}

	public static boolean isNullOrWhiteSpaceOrSpecial(String value) {
		return value == null || value.trim().isEmpty() || value.replace("\\W", "").isEmpty();
	}
}
