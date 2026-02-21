package uz.eduplatform.core.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateUtils {

    private static final ZoneId TASHKENT_ZONE = ZoneId.of("Asia/Tashkent");

    private DateUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(TASHKENT_ZONE);
    }

    public static LocalDateTime startOfDay() {
        return now().toLocalDate().atStartOfDay();
    }

    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }
}
