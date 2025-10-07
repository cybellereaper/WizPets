package com.github.cybellereaper.wizpets.nova.ast;

/** Marker interface for all Nova expressions. */
public sealed interface NovaExpression
    permits
        AsyncExpression,
        AwaitExpression,
        BlockExpression,
        BooleanLiteralExpression,
        CallExpression,
        EffectExpression,
        IdentifierExpression,
        IfExpression,
        LambdaExpression,
        ListLiteralExpression,
        MatchExpression,
        NumberLiteralExpression,
        PipeExpression,
        StringLiteralExpression
{}
