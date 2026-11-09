package io.github.happyduke96.squell.converter.postgres;

/**
 * A half-open bound pair {@code [lower, upper)}; a {@code null} bound leaves that side unbounded.
 *
 * @param lower the inclusive lower bound
 * @param upper the exclusive upper bound
 * @param <T> the bound type
 */
public record Range<T>(T lower, T upper) {
}
