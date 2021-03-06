package fr.landel.calc.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import fr.landel.calc.processor.Entity;
import fr.landel.calc.processor.Unity;

public final class DateUtils {

    public static final double NANO_PER_MICROSECOND = 1_000;
    public static final double NANO_PER_MILLISECOND = NANO_PER_MICROSECOND * 1_000;
    public static final double NANO_PER_SECOND = NANO_PER_MILLISECOND * 1_000;
    public static final double NANO_PER_MINUTE = NANO_PER_SECOND * 60;
    public static final double NANO_PER_HOUR = NANO_PER_MINUTE * 60;
    public static final double NANO_PER_DAY = NANO_PER_HOUR * 24;
    public static final double NANO_PER_WEEK = NANO_PER_DAY * 7;
    public static final double NANO_PER_YEAR = 365 * NANO_PER_DAY;
    public static final double NANO_PER_YEAR_AVG = 365.2425 * NANO_PER_DAY;
    public static final double NANO_PER_YEAR_LEAP = 366 * NANO_PER_DAY;
    public static final double NANO_PER_MONTH = NANO_PER_YEAR / 12;
    public static final double NANO_PER_MONTH_AVG = NANO_PER_YEAR_AVG / 12;
    public static final double NANO_PER_MONTH_LEAP = NANO_PER_YEAR_LEAP / 12;

    public static final double NANO_EPOCH = toZeroNanosecond(1970);

    public static final int DAYS_JANUARY = 31;
    public static final int DAYS_FEBRUARY = 28;
    public static final int DAYS_FEBRUARY_LEAP = 29;
    public static final int DAYS_MARCH = 31;
    public static final int DAYS_APRIL = 30;
    public static final int DAYS_MAY = 31;
    public static final int DAYS_JUNE = 30;
    public static final int DAYS_JULY = 31;
    public static final int DAYS_AUGUST = 31;
    public static final int DAYS_SEPTEMBER = 30;
    public static final int DAYS_OCTOBER = 31;
    public static final int DAYS_NOVEMBER = 30;
    public static final int DAYS_DECEMBER = 31;

    public static final Map<Integer, Double> NANO_PER_MONTHS;
    public static final Map<Integer, Double> NANO_PER_MONTHS_LEAP;
    public static final Map<Integer, Double> NANO_PER_MONTHS_SUM;
    public static final Map<Integer, Double> NANO_PER_MONTHS_LEAP_SUM;
    static {
        Map<Integer, Double> map = new HashMap<>();

        map.put(0, DAYS_JANUARY * NANO_PER_DAY);
        map.put(1, DAYS_FEBRUARY * NANO_PER_DAY);
        map.put(2, DAYS_MARCH * NANO_PER_DAY);
        map.put(3, DAYS_APRIL * NANO_PER_DAY);
        map.put(4, DAYS_MAY * NANO_PER_DAY);
        map.put(5, DAYS_JUNE * NANO_PER_DAY);
        map.put(6, DAYS_JULY * NANO_PER_DAY);
        map.put(7, DAYS_AUGUST * NANO_PER_DAY);
        map.put(8, DAYS_SEPTEMBER * NANO_PER_DAY);
        map.put(9, DAYS_OCTOBER * NANO_PER_DAY);
        map.put(10, DAYS_NOVEMBER * NANO_PER_DAY);
        map.put(11, DAYS_DECEMBER * NANO_PER_DAY);

        NANO_PER_MONTHS = Collections.unmodifiableMap(map);

        map = new HashMap<>(NANO_PER_MONTHS);
        map.put(1, DAYS_FEBRUARY_LEAP * NANO_PER_DAY);

        NANO_PER_MONTHS_LEAP = Collections.unmodifiableMap(map);

        map = new HashMap<>();
        double sum = 0;
        for (Entry<Integer, Double> entry : NANO_PER_MONTHS.entrySet()) {
            sum += entry.getValue();
            map.put(entry.getKey(), sum);
        }

        NANO_PER_MONTHS_SUM = Collections.unmodifiableMap(map);

        map = new HashMap<>();
        sum = 0;
        for (Entry<Integer, Double> entry : NANO_PER_MONTHS_LEAP.entrySet()) {
            sum += entry.getValue();
            map.put(entry.getKey(), sum);
        }

        NANO_PER_MONTHS_LEAP_SUM = Collections.unmodifiableMap(map);
    }

    public static final Predicate<Integer> IS_LEAP_YEAR = year -> (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);

    private DateUtils() {
        throw new UnsupportedOperationException();
    }

    public static Entity add(final Entity a, final Entity b, final Unity unity) {
        final LocalDateTime date;

        if (a.isDate() && b.isDuration()) {
            Duration bDuration = b.getDuration().get();
            date = a.getDate().get().plus(bDuration);

        } else if (a.isDuration() && b.isDate()) {
            Duration aDuration = a.getDuration().get();
            date = b.getDate().get().plus(aDuration);

        } else { // a.isDuration() && b.isDuration()
            Duration aDuration = a.getDuration().get();
            Duration bDuration = b.getDuration().get();
            Duration duration = aDuration.plus(bDuration);

            // don't use toNanos to avoid long overflow
            Double diff = Double.valueOf(duration.getSeconds() * DateUtils.NANO_PER_SECOND + duration.getNano());

            return new Entity(a.getIndex(), diff, duration, unity);
        }
        return new Entity(a.getIndex(), date.toEpochSecond(ZoneOffset.UTC) * NANO_PER_SECOND + date.getNano(), date, unity);
    }

    public static Entity subtract(final Entity a, final Entity b, final Unity unity) {
        final Double diff;
        final Duration duration;

        if (a.isDate() && b.isDate()) {
            LocalDateTime aDateTime = a.getDate().get();
            LocalDateTime bDateTime = b.getDate().get();

            diff = ChronoUnit.SECONDS.between(bDateTime, aDateTime) * NANO_PER_SECOND + aDateTime.getNano() - bDateTime.getNano();
            duration = Duration.ofNanos(diff.longValue());

        } else if (a.isDate() && b.isDuration()) {
            Duration bDuration = b.getDuration().get();
            LocalDateTime date = a.getDate().get().minus(bDuration);

            return new Entity(a.getIndex(), date.toEpochSecond(ZoneOffset.UTC) * NANO_PER_SECOND + date.getNano(), date, unity);

        } else { // a.isInterval() && b.isInterval()
            Duration aDuration = a.getDuration().get();
            Duration bDuration = b.getDuration().get();
            duration = aDuration.minus(bDuration);

            diff = Double.valueOf(duration.toNanos());
        }

        return new Entity(a.getIndex(), diff, duration, unity);
    }

    public static double fromZeroNanosecond(final Double date) {
        int leap = 0;
        int year = 0;
        for (int i = 0; NANO_PER_YEAR * (i - leap) + NANO_PER_YEAR_LEAP * leap < date; i = nextLeapYear(i)) {
            ++leap;
            year = i;
        }
        double diff = date - (NANO_PER_YEAR * (year - leap) + NANO_PER_YEAR_LEAP * leap);
        if (diff > NANO_PER_YEAR) {
            return year + Math.round(diff / NANO_PER_YEAR);
        } else {
            return year;
        }
    }

    public static double toZeroNanosecond(final LocalDateTime date) {
        return date.toEpochSecond(ZoneOffset.UTC) * DateUtils.NANO_PER_SECOND + DateUtils.NANO_EPOCH;
    }

    public static double toZeroNanosecond(final Double year) {
        return toZeroNanosecond(year.intValue());
    }

    public static double toZeroNanosecond(final int year) {
        int leap = 0;
        for (int i = 0; i < year; i = nextLeapYear(i)) {
            ++leap;
        }

        return NANO_PER_YEAR * (year - leap) + NANO_PER_YEAR_LEAP * leap;
    }

    private static int nextLeapYear(final int leapYear) {
        final int next = leapYear + 4;
        if (next % 100 != 0 || next % 400 == 0) {
            return next;
        }
        return nextLeapYear(next);
    }
}
