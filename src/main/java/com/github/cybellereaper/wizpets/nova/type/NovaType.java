package com.github.cybellereaper.wizpets.nova.type;

/** Marker interface for Nova types. */
public sealed interface NovaType
    permits FunctionType, ListType, PrimitiveType, FutureType, UnknownType, UserType {}
