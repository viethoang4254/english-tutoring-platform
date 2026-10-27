package com.englishlearning.features.course.service;

import java.time.*;
import java.util.*;

/** Pure validation and bounded weekly-stream merge; no persistence or timezone defaults. */
public final class SchedulePlan {
    private SchedulePlan() {}
    public record Rule(int dayOfWeek, LocalTime startTime, LocalTime endTime) {}
    public record Occurrence(int number, Instant start, Instant end) {}
    private record Candidate(LocalDate date, Rule rule) {}
    public static List<Rule> parse(Map<String,Object> fields) {
        if (fields == null || !fields.keySet().equals(Set.of("rules"))
                || !(fields.get("rules") instanceof List<?> rows) || rows.isEmpty() || rows.size() > 100)
            throw CourseFailure.invalid();
        var rules = new ArrayList<Rule>();
        for (Object row : rows) {
            if (!(row instanceof Map<?,?> m) || !m.keySet().equals(Set.of("dayOfWeek","startTime","endTime"))
                    || !(m.get("dayOfWeek") instanceof Integer day)) throw CourseFailure.invalid();
            rules.add(new Rule(day, time(m.get("startTime")), time(m.get("endTime"))));
        }
        return canonical(rules);
    }
    private static LocalTime time(Object value) {
        if (!(value instanceof String s) || !s.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) throw CourseFailure.invalid();
        return LocalTime.parse(s);
    }
    public static List<Rule> canonical(List<Rule> rules) {
        if (rules == null || rules.isEmpty() || rules.size() > 100) throw CourseFailure.invalid();
        for (Rule r : rules) {
            if (r == null || r.dayOfWeek < 1 || r.dayOfWeek > 7 || r.startTime == null || r.endTime == null
                    || r.startTime.getSecond() != 0 || r.startTime.getNano() != 0
                    || r.endTime.getSecond() != 0 || r.endTime.getNano() != 0 || !r.startTime.isBefore(r.endTime))
                throw CourseFailure.invalid();
        }
        var sorted = rules.stream().sorted(Comparator.comparingInt(Rule::dayOfWeek).thenComparing(Rule::startTime)).toList();
        for (int i=1;i<sorted.size();i++) {
            var a=sorted.get(i-1); var b=sorted.get(i);
            if (a.dayOfWeek == b.dayOfWeek && b.startTime.isBefore(a.endTime))
                throw new CourseFailure(CourseFailure.Reason.SCHEDULE_OVERLAP);
        }
        return sorted;
    }
    public static List<Occurrence> generate(List<Rule> rules, LocalDate earliest, String timezone, int count, Instant now) {
        if (count > 1000) throw new CourseFailure(CourseFailure.Reason.GENERATION_LIMIT);
        if (count < 1 || earliest == null || earliest.getYear() < 1 || earliest.getYear() > 9999
                || timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) throw CourseFailure.invalid();
        var zone = ZoneId.of(timezone);
        var queue = new PriorityQueue<Candidate>(Comparator.comparing(Candidate::date).thenComparing(c -> c.rule.startTime));
        try {
            for (Rule rule : canonical(rules)) queue.add(new Candidate(earliest.plusDays(Math.floorMod(rule.dayOfWeek-earliest.getDayOfWeek().getValue(),7)),rule));
            var result = new ArrayList<Occurrence>(count);
            Instant previousEnd = null;
            for (int i=1;i<=count;i++) {
                var next = queue.remove();
                if (next.date.getYear() > 9999) throw CourseFailure.invalid();
                Instant start=resolve(next.date.atTime(next.rule.startTime),zone), end=resolve(next.date.atTime(next.rule.endTime),zone);
                if (!start.isAfter(now) || !end.isAfter(start)) throw new CourseFailure(CourseFailure.Reason.SCHEDULE_TIME);
                if (previousEnd != null && start.isBefore(previousEnd)) throw new CourseFailure(CourseFailure.Reason.SCHEDULE_OVERLAP);
                result.add(new Occurrence(i,start,end)); previousEnd=end;
                if (i<count) queue.add(new Candidate(next.date.plusWeeks(1),next.rule));
            }
            return List.copyOf(result);
        } catch (DateTimeException | ArithmeticException e) { throw CourseFailure.invalid(); }
    }
    private static Instant resolve(LocalDateTime local, ZoneId zone) {
        var offsets=zone.getRules().getValidOffsets(local);
        if (offsets.size()!=1) throw new CourseFailure(CourseFailure.Reason.SCHEDULE_TIME);
        return local.toInstant(offsets.get(0));
    }
}
