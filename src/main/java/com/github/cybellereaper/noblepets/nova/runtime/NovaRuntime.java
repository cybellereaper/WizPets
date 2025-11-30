package com.github.cybellereaper.noblepets.nova.runtime;

import com.github.cybellereaper.noblepets.nova.ast.NovaExpression;
import com.github.cybellereaper.noblepets.nova.ast.NovaProgram;
import com.github.cybellereaper.noblepets.nova.type.NovaType;
import com.github.cybellereaper.noblepets.nova.type.TypeChecker;
import com.github.cybellereaper.noblepets.nova.type.TypeEnvironment;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** High level entry point tying together typing and evaluation. */
public final class NovaRuntime implements AutoCloseable {
  private final ExecutorService executor;
  private final boolean ownsExecutor;
  private final TypeEnvironment typeEnvironment;
  private final TypeChecker typeChecker;
  private final NovaEnvironment valueEnvironment;
  private final NovaEvaluator evaluator;

  private NovaRuntime(ExecutorService executor, boolean ownsExecutor) {
    this.executor = executor;
    this.ownsExecutor = ownsExecutor;
    this.typeEnvironment = new TypeEnvironment();
    this.typeChecker = new TypeChecker(typeEnvironment);
    this.valueEnvironment = new NovaEnvironment();
    this.evaluator = new NovaEvaluator(valueEnvironment, executor);
  }

  public static NovaRuntime createDefault() {
    return new NovaRuntime(Executors.newVirtualThreadPerTaskExecutor(), true);
  }

  public static NovaRuntime create(ExecutorService executor) {
    Objects.requireNonNull(executor, "executor");
    return new NovaRuntime(executor, false);
  }

  public void load(NovaProgram program) {
    typeChecker.checkProgram(program);
    evaluator.executeProgram(program);
  }

  public NovaValue evaluate(NovaExpression expression) {
    return evaluator.evaluate(expression, valueEnvironment);
  }

  public void registerHostFunction(String name, NovaCallable callable, NovaType type) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(callable, "callable");
    Objects.requireNonNull(type, "type");
    typeEnvironment.defineValue(name, type);
    valueEnvironment.define(name, callable);
  }

  public TypeEnvironment typeEnvironment() {
    return typeEnvironment;
  }

  public NovaEnvironment valueEnvironment() {
    return valueEnvironment;
  }

  @Override
  public void close() {
    if (ownsExecutor) {
      executor.shutdownNow();
    }
  }
}
