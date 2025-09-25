// src/main/java/com/dodam/plan/util/PlanDurationPolicy.java
package com.dodam.plan.util;

import java.time.*;

public final class PlanDurationPolicy {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private PlanDurationPolicy() {}

    public static ZonedDateTime resolveStart(ZonedDateTime paidAtKst, ZonedDateTime currentEndKst) {
        if (currentEndKst == null) return paidAtKst;
        return paidAtKst.isAfter(currentEndKst) ? paidAtKst : currentEndKst;
    }

    public static ZonedDateTime plusMonthsEomAware(ZonedDateTime start, int months) {
        LocalDateTime ldt = start.toLocalDateTime();
        LocalDate date = ldt.toLocalDate();
        LocalTime time = ldt.toLocalTime();

        LocalDate target = date.plusMonths(months);
        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            target = target.withDayOfMonth(target.lengthOfMonth());
        } else {
            int dom = Math.min(date.getDayOfMonth(), target.lengthOfMonth());
            target = target.withDayOfMonth(dom);
        }
        return ZonedDateTime.of(LocalDateTime.of(target, time), start.getZone());
    }

    public static ZonedDateTime toKst(Instant instant) { return instant.atZone(ZONE); }
}
