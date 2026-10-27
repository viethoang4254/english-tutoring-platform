package com.englishlearning.features.course;

import com.englishlearning.features.course.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CourseInputTests {
    public static Map<String,Object> fields() {
        return new HashMap<>(Map.of("categoryId", "1", "name", "  English  ", "description", "Description",
                "tuition", Map.of("amount", "1500000.00", "currency", "VND"), "minStudents", 1,
                "maxStudents", 150, "plannedStartDate", "2020-01-01", "timezone", "Asia/Ho_Chi_Minh", "sessionCount", 200));
    }
    @Test void validLimitsAndNoInventedCapacityCap() {
        var f = fields(); f.put("maxStudents", Integer.MAX_VALUE); f.put("sessionCount", Integer.MAX_VALUE);
        f.put("tuition", Map.of("amount", "9999999999999.99", "currency", "VND"));
        var value = CourseInput.merge(f, null);
        assertEquals("English", value.name()); assertEquals(Integer.MAX_VALUE, value.sessionCount());
        assertEquals("9999999999999.99", value.tuition().toPlainString());
    }
    @Test void rejectsUnknownNullMissingAndProtectedFields() {
        for (String key : fields().keySet()) {
            var f = fields(); f.remove(key); assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null));
            f.put(key, null); assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null));
        }
        for (String key : List.of("teacherId", "status", "meetUrl", "thumbnailPath", "id")) {
            var f = fields(); f.put(key, "1"); assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null));
        }
    }
    @Test void moneyPrecisionAndTypesAreStrict() {
        for (Object amount : List.of("-1", "1.001", "1e3", "NaN", "10000000000000", 1, " 1")) {
            var f = fields(); f.put("tuition", Map.of("amount", amount, "currency", "VND"));
            assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null));
        }
        var f = fields(); f.put("tuition", Map.of("amount", "0", "currency", "VND"));
        assertEquals("0.00", CourseInput.merge(f, null).tuition().toPlainString());
    }
    @Test void textDateZoneAndIntegerValidation() {
        Map<String,List<Object>> invalid = Map.of("name", List.of(" ", "a\u0000", "x".repeat(201), "\uD800"),
                "description", List.of("\n\t", "x".repeat(10001), "a\u0000"),
                "plannedStartDate", List.of("2026-02-30", "0000-01-01", "2026-1-1"),
                "timezone", List.of("+07:00", "not/a-zone"), "maxStudents", List.of(0, 1.0, "1", 2147483648L),
                "sessionCount", List.of(0, -1), "categoryId", List.of(1, "0", "-1", "9223372036854775808"));
        invalid.forEach((key, values) -> values.forEach(value -> {
            var f = fields(); f.put(key, value); assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null), key);
        }));
        var f = fields(); f.put("name", "😀".repeat(200)); f.put("description", "Line\nTwo\tOK");
        assertEquals(400, CourseInput.merge(f, null).name().length());
    }
    @Test void completeDiscountAndMicrosecondPrecision() {
        var f = fields();
        f.put("discount", Map.of("percent", "10", "startAt", "2026-01-01T00:00:00+07:00", "endAt", "2026-02-01T00:00:00Z"));
        assertEquals("10.00", CourseInput.merge(f, null).discount().percent());
        for (var discount : List.of(Map.of("percent", "10"),
                Map.of("percent", "100", "startAt", "2026-01-01T00:00:00Z", "endAt", "2026-02-01T00:00:00Z"),
                Map.of("percent", "10", "startAt", "2026-02-01T00:00:00Z", "endAt", "2026-01-01T00:00:00Z"),
                Map.of("percent", "10", "startAt", "2026-01-01T00:00:00.0000001Z", "endAt", "2026-02-01T00:00:00Z"))) {
            f.put("discount", discount); assertThrows(CourseFailure.class, () -> CourseInput.merge(f, null));
        }
    }
}
