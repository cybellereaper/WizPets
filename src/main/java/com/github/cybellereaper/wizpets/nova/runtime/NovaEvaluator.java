package com.github.cybellereaper.wizpets.nova.runtime;

import com.github.cybellereaper.wizpets.nova.ast.AsyncExpression;
import com.github.cybellereaper.wizpets.nova.ast.AwaitExpression;
import com.github.cybellereaper.wizpets.nova.ast.BlockExpression;
import com.github.cybellereaper.wizpets.nova.ast.BooleanLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.CallExpression;
import com.github.cybellereaper.wizpets.nova.ast.EffectExpression;
import com.github.cybellereaper.wizpets.nova.ast.FunctionDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.IdentifierExpression;
import com.github.cybellereaper.wizpets.nova.ast.IfExpression;
import com.github.cybellereaper.wizpets.nova.ast.LambdaExpression;
import com.github.cybellereaper.wizpets.nova.ast.LetDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.ListLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.MatchArm;
import com.github.cybellereaper.wizpets.nova.ast.MatchExpression;
import com.github.cybellereaper.wizpets.nova.ast.NovaDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.NovaExpression;
import com.github.cybellereaper.wizpets.nova.ast.NovaProgram;
import com.github.cybellereaper.wizpets.nova.ast.NumberLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.Parameter;
import com.github.cybellereaper.wizpets.nova.ast.PipeExpression;
import com.github.cybellereaper.wizpets.nova.ast.StringLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.TypeDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Evaluates Nova AST nodes to runtime values. */
public final class NovaEvaluator {
  private final NovaEnvironment globals;
  private final Executor executor;

  public NovaEvaluator(NovaEnvironment globals, Executor executor) {
    this.globals = globals;
    this.executor = executor;
  }

  public void executeProgram(NovaProgram program) {
    for (NovaDeclaration declaration : program.declarations()) {
      switch (declaration) {
        case TypeDeclaration ignored -> {
          // Types are erased at runtime.
        }
        case LetDeclaration letDeclaration -> {
          NovaValue value = evaluate(letDeclaration.expression(), globals);
          globals.define(letDeclaration.name(), value);
        }
        case FunctionDeclaration functionDeclaration ->
            globals.define(
                functionDeclaration.name(),
                new NovaFunctionValue(
                    functionDeclaration.parameters(),
                    functionDeclaration.body(),
                    globals,
                    this));
      }
    }
  }

  public NovaValue evaluate(NovaExpression expression, NovaEnvironment environment) {
    return switch (expression) {
      case NumberLiteralExpression literal -> new NovaNumberValue(literal.value());
      case StringLiteralExpression literal -> new NovaStringValue(literal.value());
      case BooleanLiteralExpression literal -> new NovaBooleanValue(literal.value());
      case ListLiteralExpression list -> evaluateList(list, environment);
      case IdentifierExpression identifier ->
          environment
              .lookup(identifier.name())
              .orElseThrow(
                  () ->
                      new NovaEvaluationException("Unknown identifier: " + identifier.name()));
      case LambdaExpression lambda ->
          new NovaFunctionValue(lambda.parameters(), lambda.body(), environment, this);
      case CallExpression call -> evaluateCall(call, environment);
      case BlockExpression block -> evaluateBlock(block, environment);
      case IfExpression ifExpression -> evaluateIf(ifExpression, environment);
      case AsyncExpression asyncExpression -> evaluateAsync(asyncExpression, environment);
      case AwaitExpression awaitExpression -> evaluateAwait(awaitExpression, environment);
      case EffectExpression effectExpression -> evaluate(effectExpression.expression(), environment);
      case PipeExpression pipeExpression -> evaluatePipe(pipeExpression, environment);
      case MatchExpression matchExpression -> evaluateMatch(matchExpression, environment);
    };
  }

  public NovaValue invokeFunction(NovaFunctionValue function, List<NovaValue> arguments) {
    if (function.parameters().size() != arguments.size()) {
      throw new NovaEvaluationException("Function arity mismatch");
    }
    NovaEnvironment scope = function.closure().createChild();
    for (int i = 0; i < function.parameters().size(); i++) {
      scope.define(function.parameters().get(i).name(), arguments.get(i));
    }
    return evaluate(function.body(), scope);
  }

  private NovaValue evaluateList(ListLiteralExpression expression, NovaEnvironment env) {
    List<NovaValue> values = new ArrayList<>();
    for (NovaExpression element : expression.elements()) {
      values.add(evaluate(element, env));
    }
    return new NovaListValue(values);
  }

  private NovaValue evaluateCall(CallExpression expression, NovaEnvironment env) {
    NovaValue callee = evaluate(expression.callee(), env);
    if (!(callee instanceof NovaCallable callable)) {
      throw new NovaEvaluationException("Attempted to call a non-callable value");
    }
    List<NovaValue> arguments = new ArrayList<>();
    for (NovaExpression argument : expression.arguments()) {
      arguments.add(evaluate(argument, env));
    }
    return callable.invoke(arguments);
  }

  private NovaValue evaluateBlock(BlockExpression expression, NovaEnvironment env) {
    NovaEnvironment scope = env.createChild();
    NovaValue last = NovaUnitValue.INSTANCE;
    for (NovaExpression entry : expression.expressions()) {
      last = evaluate(entry, scope);
    }
    return last;
  }

  private NovaValue evaluateIf(IfExpression expression, NovaEnvironment env) {
    NovaValue condition = evaluate(expression.condition(), env);
    if (!(condition instanceof NovaBooleanValue booleanValue)) {
      throw new NovaEvaluationException("If condition must evaluate to a boolean");
    }
    if (booleanValue.value()) {
      return evaluate(expression.thenBranch(), env);
    }
    return expression.elseBranch().map(expr -> evaluate(expr, env)).orElse(NovaUnitValue.INSTANCE);
  }

  private NovaValue evaluateAsync(AsyncExpression expression, NovaEnvironment env) {
    NovaEnvironment scope = env.createChild();
    CompletableFuture<NovaValue> future =
        CompletableFuture.supplyAsync(() -> evaluate(expression.block(), scope), executor);
    return new NovaFutureValue(future);
  }

  private NovaValue evaluateAwait(AwaitExpression expression, NovaEnvironment env) {
    NovaValue awaited = evaluate(expression.expression(), env);
    if (!(awaited instanceof NovaFutureValue futureValue)) {
      throw new NovaEvaluationException("Await requires a future value");
    }
    try {
      return futureValue.future().join();
    } catch (Exception exception) {
      throw new NovaEvaluationException("Failed to await future", exception);
    }
  }

  private NovaValue evaluatePipe(PipeExpression expression, NovaEnvironment env) {
    NovaValue current = evaluate(expression.seed(), env);
    for (CallExpression stage : expression.stages()) {
      NovaEnvironment scope = env.createChild();
      scope.define("it", current);
      current = evaluateCall(stage, scope);
    }
    return current;
  }

  private NovaValue evaluateMatch(MatchExpression expression, NovaEnvironment env) {
    NovaValue target = evaluate(expression.target(), env);
    for (MatchArm arm : expression.arms()) {
      if (target instanceof NovaVariantValue variant && variant.constructor().equals(arm.constructor())) {
        return executeMatchArm(env, arm, variant.fields());
      }
      if (target instanceof NovaStringValue stringValue
          && stringValue.value().equals(arm.constructor())) {
        return executeMatchArm(env, arm, List.of());
      }
    }
    throw new NovaEvaluationException("No matching arm for value: " + target);
  }

  private NovaValue executeMatchArm(NovaEnvironment env, MatchArm arm, List<NovaValue> fields) {
    NovaEnvironment scope = env.createChild();
    if (arm.destructured().isPresent()) {
      List<Parameter> parameters = arm.destructured().get();
      if (parameters.size() != fields.size()) {
        throw new NovaEvaluationException("Match arm destructuring does not match value arity");
      }
      for (int i = 0; i < parameters.size(); i++) {
        scope.define(parameters.get(i).name(), fields.get(i));
      }
    }
    return evaluate(arm.body(), scope);
  }
}
