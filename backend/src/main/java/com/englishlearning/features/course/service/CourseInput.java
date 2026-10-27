package com.englishlearning.features.course.service;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.course.dto.CourseDto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.englishlearning.features.course.service.CourseFailure.invalid;

public record CourseInput(long categoryId, String name, String description, BigDecimal tuition,
        int minStudents, int maxStudents, LocalDate plannedStartDate, String timezone,
        int sessionCount, CourseDto.Discount discount) {
    private static final Set<String> REQUIRED = Set.of("categoryId", "name", "description", "tuition",
            "minStudents", "maxStudents", "plannedStartDate", "timezone", "sessionCount");
    private static final Set<String> ALLOWED = Set.of("categoryId", "name", "description", "tuition",
            "minStudents", "maxStudents", "plannedStartDate", "timezone", "sessionCount", "discount");
    public static CourseInput merge(Map<String, Object> fields, CourseDto current) {
        if (fields == null || fields.isEmpty() || !ALLOWED.containsAll(fields.keySet())
                || current == null && !fields.keySet().containsAll(REQUIRED)) throw invalid();
        Map<String, Object> values = new HashMap<>();
        if (current != null) {
            values.put("categoryId", current.category().id().decimal());
            values.put("name", current.name()); values.put("description", current.description());
            values.put("tuition", Map.of("amount", current.tuition().amount(), "currency", current.tuition().currency()));
            values.put("minStudents", current.minStudents()); values.put("maxStudents", current.maxStudents());
            values.put("plannedStartDate", current.plannedStartDate().toString()); values.put("timezone", current.timezone());
            values.put("sessionCount", current.sessionCount());
            if (current.discount() != null) values.put("discount", Map.of("percent", current.discount().percent(),
                    "startAt", current.discount().startAt().toString(), "endAt", current.discount().endAt().toString()));
        }
        values.putAll(fields);
        var money = object(values.get("tuition"), Set.of("amount", "currency"));
        if (!"VND".equals(money.get("currency"))) throw invalid();
        BigDecimal amount = decimal(money.get("amount"), new BigDecimal("9999999999999.99"));
        int min = integer(values.get("minStudents")), max = integer(values.get("maxStudents"));
        if (max < min) throw invalid();
        String date = string(values.get("plannedStartDate"));
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalid();
        LocalDate planned;
        try { planned = LocalDate.parse(date); } catch (DateTimeException e) { throw invalid(); }
        if (planned.getYear() < 1) throw invalid();
        String zone = string(values.get("timezone"));
        if (!ZoneId.getAvailableZoneIds().contains(zone)) throw invalid();
        CourseDto.Discount discount = null;
        if (values.get("discount") != null) {
            var d = object(values.get("discount"), Set.of("percent", "startAt", "endAt"));
            BigDecimal percent = decimal(d.get("percent"), new BigDecimal("99.99"));
            if (percent.signum() == 0) throw invalid();
            Instant start = instant(d.get("startAt")), end = instant(d.get("endAt"));
            if (!start.isBefore(end)) throw invalid();
            discount = new CourseDto.Discount(percent.toPlainString(), start, end);
        }
        return new CourseInput(id(values.get("categoryId")), text(values.get("name"), 200, false),
                text(values.get("description"), 10000, true), amount, min, max, planned, zone,
                integer(values.get("sessionCount")), discount);
    }
    public static long id(Object value) {
        try { long id = ApiId.parse(string(value)).value(); if (id <= 0) throw invalid(); return id; }
        catch (IllegalArgumentException e) { throw invalid(); }
    }
    private static String string(Object v) { if (!(v instanceof String s)) throw invalid(); return s; }
    private static Map<?, ?> object(Object v, Set<String> keys) {
        if (!(v instanceof Map<?, ?> map) || !map.keySet().equals(keys)) throw invalid(); return map;
    }
    private static int integer(Object v) { if (!(v instanceof Integer i) || i < 1) throw invalid(); return i; }
    private static BigDecimal decimal(Object v, BigDecimal max) {
        String s = string(v);
        if (!s.matches("[0-9]+(?:\\.[0-9]{1,2})?")) throw invalid();
        BigDecimal n = new BigDecimal(s);
        if (n.compareTo(max) > 0) throw invalid(); return n.setScale(2);
    }
    private static Instant instant(Object v) {
        try {
            Instant i = OffsetDateTime.parse(string(v)).toInstant();
            if (i.getNano() % 1000 != 0 || i.atOffset(ZoneOffset.UTC).getYear() < 1
                    || i.atOffset(ZoneOffset.UTC).getYear() > 9999) throw invalid(); return i;
        } catch (DateTimeException e) { throw invalid(); }
    }
    static String text(Object v, int limit, boolean multiline) {
        String s = string(v); int start = 0, end = s.length();
        while (start < end && s.charAt(start) == ' ') start++;
        while (end > start && s.charAt(end - 1) == ' ') end--;
        s = s.substring(start, end);
        if (s.isBlank() || s.codePointCount(0, s.length()) > limit || s.codePoints().anyMatch(c ->
                c >= 0xD800 && c <= 0xDFFF || Character.isISOControl(c)
                && !(multiline && (c == '\n' || c == '\r' || c == '\t')))) throw invalid();
        return s;
    }
}
