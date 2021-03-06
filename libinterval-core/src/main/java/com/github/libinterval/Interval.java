package com.github.libinterval;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.libinterval.TemporalConverters.convertLowerEndpoint;
import static com.github.libinterval.TemporalConverters.convertUpperEndpoint;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Represents interval between two inclusive endpoints or in some cases interval witch has gap(s).
 *
 * @param <T> - type which implements both Comparable and Temporal. So it supports such types
 *            like {@link LocalDate}, {@link YearMonth}...
 * @author Igor Rybak
 * @since 15-Sep-2018
 */
public interface Interval<T extends Comparable<?> & Temporal> {

    /**
     * @param lowerEndpoint - nullable lower endpoint. If null than -∞.
     * @return [[lowerEndpoint..+∞)].
     */
    static <T extends Comparable<?> & Temporal> Interval<T> from(T lowerEndpoint) {
        return between(lowerEndpoint, null);
    }

    /**
     * @param upperEndpoint - nullable upper endpoint. If null than +∞.
     * @return [(- ∞ ..upperEndpoint]].
     */
    static <T extends Comparable<?> & Temporal> Interval<T> to(T upperEndpoint) {
        return between(null, upperEndpoint);
    }

    /**
     * @param lowerEndpoint - nullable lower endpoint. If null than -∞.
     * @param upperEndpoint - nullable upper endpoint. If null than +∞.
     * @return [[lowerEndpoint..upperEndpoint]]
     */
    static <T extends Comparable<?> & Temporal> Interval<T> between(T lowerEndpoint, T upperEndpoint) {
        Range<T> range;
        if (lowerEndpoint != null && upperEndpoint != null) {
            range = Range.closed(lowerEndpoint, upperEndpoint);
        } else if (lowerEndpoint != null) {
            range = Range.atLeast(lowerEndpoint);
        } else if (upperEndpoint != null) {
            range = Range.atMost(upperEndpoint);
        } else {
            range = Range.all();
        }

        return new IntervalImpl<>(ImmutableRangeSet.of(range));
    }

    /**
     * @param lowerEndpoint - required lower endpoint.
     * @return [[lowerEndpoint..+∞)].
     */
    static <T extends Comparable<?> & Temporal> Interval<T> atLeast(T lowerEndpoint) {
        return new IntervalImpl<>(ImmutableRangeSet.of(Range.atLeast(lowerEndpoint)));
    }

    /**
     * @param upperEndpoint - required lower endpoint.
     * @return [(- ∞ ..upperEndpoint]].
     */
    static <T extends Comparable<?> & Temporal> Interval<T> atMost(T upperEndpoint) {
        return new IntervalImpl<>(ImmutableRangeSet.of(Range.atMost(upperEndpoint)));
    }

    /**
     * @param lowerEndpoint - required lower endpoint.
     * @param upperEndpoint - required upper endpoint.
     * @return [[lowerEndpoint..upperEndpoint]]
     */
    static <T extends Comparable<?> & Temporal> Interval<T> closed(T lowerEndpoint, T upperEndpoint) {
        return new IntervalImpl<>(ImmutableRangeSet.of(Range.closed(lowerEndpoint, upperEndpoint)));
    }

    /**
     * Finds interval of intersection of several intervals.
     *
     * @param intervals - intervals to find intersection.
     * @return intersection of intervals or Optional.empty() if there is no intersection.
     */
    @SafeVarargs
    static <T extends Comparable<?> & Temporal> Interval<T> intersectionOf(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return IntervalUtils.intersection(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    static <T extends Comparable<?> & Temporal> Interval<T> intersectionOf(Iterable<Interval<T>> intervals) {
        return IntervalUtils.intersection(Streams.stream(intervals));
    }

    /**
     * Creates union of several intervals. E.g:
     * interval1 = [2018-03-15..2018-03-25],
     * interval2 = [2018-04-04..2018-04-14].
     * So in this case result is interval [2018-03-15..2018-04-14] with exclusive gap [2018-03-25..2018-04-04].
     *
     * @param intervals - intervals to create union;
     */
    @SafeVarargs
    static <T extends Comparable<?> & Temporal> Interval<T> unionOf(Interval<T> first, Interval<T> second, Interval<T>... intervals) {
        return IntervalUtils.union(Stream.concat(Stream.of(first, second), Arrays.stream(intervals)));
    }

    static <T extends Comparable<?> & Temporal> Interval<T> unionOf(Iterable<Interval<T>> intervals) {
        return IntervalUtils.union(Streams.stream(intervals));
    }

    static <T extends Comparable<?> & Temporal, V> Interval<T> unionOf(Function<V, Interval<T>> getIntervalFunction, Iterable<V> intervals) {
        return IntervalUtils.union(Streams.stream(intervals).map(getIntervalFunction));
    }

    static <T extends Comparable<?> & Temporal> Interval<T> all() {
        ImmutableRangeSet<T> all = ImmutableRangeSet.of(Range.all());
        return new IntervalImpl<>(all);
    }

    static <T extends Comparable<?> & Temporal> Interval<T> none() {
        return new IntervalImpl<>(ImmutableRangeSet.of());
    }

    /**
     * @return difference of two intervals.
     * e.g the result of difference [[2018-05-01..2018-05-10]] and [[2018-05-03..2018-05-06]]
     * is [[2018-05-01..2018-05-02], [2018-05-07..2018-05-10]]
     */
    Interval<T> difference(Interval<T> interval);

    Interval<T> difference(Interval<T> interval, TemporalUnit temporalUnit);

    Optional<T> findLowerEndpoint();

    Optional<T> findUpperEndpoint();

    boolean contains(T t);

    boolean hasLowerBound();

    boolean hasUpperBound();

    Set<Interval<T>> getSubIntervals();

    default Interval<YearMonth> toMonthsInterval() {
        return map(t -> convertLowerEndpoint(t, YearMonth.class), t -> convertUpperEndpoint(t, YearMonth.class));
    }

    default Interval<LocalDate> toDaysInterval() {
        return map(t -> convertLowerEndpoint(t, LocalDate.class), t -> convertUpperEndpoint(t, LocalDate.class));
    }

    default Interval<LocalDateTime> toTimeInterval() {
        return map(t -> convertLowerEndpoint(t, LocalDateTime.class), t -> convertUpperEndpoint(t, LocalDateTime.class));
    }

    default <R extends Comparable<?> & Temporal> Interval<R> map(Function<T, R> mapper) {
        return map(mapper, mapper);
    }

    <R extends Comparable<?> & Temporal> Interval<R> map(Function<T, R> lowerEndpointMapper,
                                                         Function<T, R> upperEndpointMapper);

    default Stream<YearMonth> months() {
        return iterate(MONTHS, t -> convertLowerEndpoint(t, YearMonth.class), t -> convertUpperEndpoint(t, YearMonth.class));
    }

    default Stream<LocalDate> days() {
        return iterate(DAYS, t -> convertLowerEndpoint(t, LocalDate.class), t -> convertUpperEndpoint(t, LocalDate.class));
    }

    default Stream<T> iterate(TemporalUnit temporalUnit) {
        return iterate(temporalUnit, null);
    }

    default <R extends Comparable<?> & Temporal> Stream<R> iterate(TemporalUnit temporalUnit,
                                                                   Function<T, R> mapper) {
        return iterate(temporalUnit, mapper, mapper);
    }

    <R extends Comparable<?> & Temporal> Stream<R> iterate(TemporalUnit temporalUnit,
                                                           Function<T, R> lowerEndpointMapper,
                                                           Function<T, R> upperEndpointMapper);

    <R> Stream<R> iterate(BiFunction<T, T, Stream<R>> streamGenerator);

    default long countDays() {
        return count(DAYS);
    }

    long count(TemporalUnit temporalUnit);

    boolean isPresent();

    Optional<Interval<T>> getNotNoneInterval();

    ImmutableRangeSet<T> getRangeSet();
}
