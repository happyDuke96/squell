package io.github.happyduke96.squell.converter.postgres;

/// A half-open bound pair `[lower, upper)`; either may be null for an unbounded side.
public record Range<T>(T lower, T upper) {
}
