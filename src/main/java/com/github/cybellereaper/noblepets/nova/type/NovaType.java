package com.github.cybellereaper.noblepets.nova.type;

/** Marker interface for Nova types. */
public sealed interface NovaType
    permits FunctionType, ListType, PrimitiveType, FutureType, UnknownType, UserType {}
