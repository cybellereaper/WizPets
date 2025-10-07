package com.github.cybellereaper.wizpets.nova.runtime;

/** Marker interface for runtime values produced by the Nova interpreter. */
public sealed interface NovaValue
    permits
        NovaBooleanValue,
        NovaCallable,
        NovaFutureValue,
        NovaJavaValue,
        NovaListValue,
        NovaNumberValue,
        NovaStringValue,
        NovaUnitValue,
        NovaVariantValue {}
