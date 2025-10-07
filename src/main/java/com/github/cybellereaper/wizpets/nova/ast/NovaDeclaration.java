package com.github.cybellereaper.wizpets.nova.ast;

/** Marker interface for top-level Nova declarations. */
public sealed interface NovaDeclaration permits FunctionDeclaration, LetDeclaration, TypeDeclaration {}
