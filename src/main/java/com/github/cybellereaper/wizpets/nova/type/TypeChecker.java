package com.github.cybellereaper.wizpets.nova.type;

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
import com.github.cybellereaper.wizpets.nova.ast.ModuleDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.NovaDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.NovaExpression;
import com.github.cybellereaper.wizpets.nova.ast.NovaProgram;
import com.github.cybellereaper.wizpets.nova.ast.NumberLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.Parameter;
import com.github.cybellereaper.wizpets.nova.ast.PipeExpression;
import com.github.cybellereaper.wizpets.nova.ast.StringLiteralExpression;
import com.github.cybellereaper.wizpets.nova.ast.TypeDeclaration;
import com.github.cybellereaper.wizpets.nova.ast.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Performs static analysis over Nova AST nodes. */
public final class TypeChecker {
  private final TypeEnvironment globals;

  public TypeChecker(TypeEnvironment globals) {
    this.globals = globals;
    installBuiltins(globals);
  }

  private static void installBuiltins(TypeEnvironment env) {
    env.defineType("Number", PrimitiveType.NUMBER);
    env.defineType("String", PrimitiveType.STRING);
    env.defineType("Boolean", PrimitiveType.BOOLEAN);
    env.defineType("Unit", PrimitiveType.UNIT);
    env.defineType("List", new ListType(UnknownType.INSTANCE));
    env.defineType("Future", new FutureType(UnknownType.INSTANCE));
  }

  public void checkProgram(NovaProgram program) {
    ModuleDeclaration module = program.module();
    if (module.pathSegments().isEmpty()) {
      throw new NovaTypeException("Module path must have at least one segment");
    }
    for (NovaDeclaration declaration : program.declarations()) {
      switch (declaration) {
        case TypeDeclaration typeDeclaration -> visitTypeDeclaration(typeDeclaration);
        case LetDeclaration letDeclaration -> visitLetDeclaration(letDeclaration);
        case FunctionDeclaration functionDeclaration -> visitFunctionDeclaration(functionDeclaration);
        default -> throw new IllegalStateException("Unexpected declaration: " + declaration);
      }
    }
  }

  private void visitTypeDeclaration(TypeDeclaration declaration) {
    globals.defineType(declaration.name(), new UserType(declaration.name(), declaration.variants()));
  }

  private void visitLetDeclaration(LetDeclaration declaration) {
    NovaType inferred = typeOf(declaration.expression(), globals);
    NovaType finalType =
        declaration
            .type()
            .map(
                annotation -> {
                  NovaType expected = resolve(annotation);
                  ensureAssignable(
                      expected, inferred, "Let binding type mismatch for " + declaration.name());
                  return expected;
                })
            .orElse(inferred);
    globals.defineValue(declaration.name(), finalType);
  }

  private void visitFunctionDeclaration(FunctionDeclaration declaration) {
    List<NovaType> parameterTypes = new ArrayList<>();
    for (Parameter parameter : declaration.parameters()) {
      NovaType type =
          parameter.type().map(this::resolve).orElse(UnknownType.INSTANCE);
      parameterTypes.add(type);
    }
    NovaType declaredReturn =
        declaration.returnType().map(this::resolve).orElse(UnknownType.INSTANCE);
    FunctionType signature = new FunctionType(parameterTypes, declaredReturn, false);
    globals.defineValue(declaration.name(), signature);

    TypeEnvironment functionEnv = globals.createChild();
    for (int i = 0; i < declaration.parameters().size(); i++) {
      functionEnv.defineValue(declaration.parameters().get(i).name(), parameterTypes.get(i));
    }
    NovaType actualReturn = typeOf(declaration.body(), functionEnv);
    if (declaredReturn instanceof UnknownType && actualReturn instanceof FutureType futureType) {
      globals.defineValue(
          declaration.name(), new FunctionType(parameterTypes, futureType, true));
      return;
    }
    if (!(declaredReturn instanceof UnknownType)) {
      ensureAssignable(
          declaredReturn, actualReturn, "Function return type mismatch for " + declaration.name());
    }
    boolean asynchronous = actualReturn instanceof FutureType;
    NovaType finalReturn = declaredReturn instanceof UnknownType ? actualReturn : declaredReturn;
    globals.defineValue(
        declaration.name(), new FunctionType(parameterTypes, finalReturn, asynchronous));
  }

  private NovaType typeOf(NovaExpression expression, TypeEnvironment env) {
    return switch (expression) {
      case NumberLiteralExpression ignored -> PrimitiveType.NUMBER;
      case StringLiteralExpression ignored -> PrimitiveType.STRING;
      case BooleanLiteralExpression ignored -> PrimitiveType.BOOLEAN;
      case ListLiteralExpression list -> typeOfList(list, env);
      case IdentifierExpression identifier ->
          env.lookupValue(identifier.name())
              .orElseThrow(
                  () -> new NovaTypeException("Unknown identifier: " + identifier.name()));
      case LambdaExpression lambda -> typeOfLambda(lambda, env);
      case CallExpression call -> typeOfCall(call, env);
      case BlockExpression block -> typeOfBlock(block, env);
      case IfExpression ifExpression -> typeOfIf(ifExpression, env);
      case AsyncExpression asyncExpression -> typeOfAsync(asyncExpression, env);
      case AwaitExpression awaitExpression -> typeOfAwait(awaitExpression, env);
      case EffectExpression effectExpression -> typeOf(effectExpression.expression(), env);
      case PipeExpression pipeExpression -> typeOfPipe(pipeExpression, env);
      case MatchExpression matchExpression -> typeOfMatch(matchExpression, env);
    };
  }

  private NovaType typeOfList(ListLiteralExpression list, TypeEnvironment env) {
    NovaType element = UnknownType.INSTANCE;
    for (NovaExpression expression : list.elements()) {
      NovaType candidate = typeOf(expression, env);
      element = unify(element, candidate);
    }
    return new ListType(element);
  }

  private NovaType typeOfLambda(LambdaExpression lambda, TypeEnvironment env) {
    TypeEnvironment scope = env.createChild();
    List<NovaType> parameterTypes = new ArrayList<>();
    for (Parameter parameter : lambda.parameters()) {
      NovaType type = parameter.type().map(this::resolve).orElse(UnknownType.INSTANCE);
      parameterTypes.add(type);
      scope.defineValue(parameter.name(), type);
    }
    NovaType bodyType = typeOf(lambda.body(), scope);
    return new FunctionType(parameterTypes, bodyType, bodyType instanceof FutureType);
  }

  private NovaType typeOfCall(CallExpression call, TypeEnvironment env) {
    NovaType calleeType = typeOf(call.callee(), env);
    if (!(calleeType instanceof FunctionType functionType)) {
      throw new NovaTypeException("Attempted to call non-function type: " + calleeType);
    }
    if (functionType.parameters().size() != call.arguments().size()) {
      throw new NovaTypeException("Arity mismatch for function call");
    }
    for (int i = 0; i < call.arguments().size(); i++) {
      NovaType expected = functionType.parameters().get(i);
      NovaType actual = typeOf(call.arguments().get(i), env);
      ensureAssignable(expected, actual, "Argument " + i + " does not match parameter type");
    }
    return functionType.returnType();
  }

  private NovaType typeOfBlock(BlockExpression block, TypeEnvironment env) {
    NovaType lastType = PrimitiveType.UNIT;
    TypeEnvironment scope = env.createChild();
    for (NovaExpression expression : block.expressions()) {
      lastType = typeOf(expression, scope);
    }
    return lastType;
  }

  private NovaType typeOfIf(IfExpression expression, TypeEnvironment env) {
    NovaType conditionType = typeOf(expression.condition(), env);
    ensureAssignable(PrimitiveType.BOOLEAN, conditionType, "If condition must be Boolean");
    NovaType thenType = typeOf(expression.thenBranch(), env);
    NovaType elseType = expression.elseBranch().map(e -> typeOf(e, env)).orElse(PrimitiveType.UNIT);
    return unify(thenType, elseType);
  }

  private NovaType typeOfAsync(AsyncExpression expression, TypeEnvironment env) {
    NovaType blockType = typeOf(expression.block(), env);
    return new FutureType(blockType);
  }

  private NovaType typeOfAwait(AwaitExpression expression, TypeEnvironment env) {
    NovaType awaited = typeOf(expression.expression(), env);
    if (awaited instanceof FutureType futureType) {
      return futureType.innerType();
    }
    throw new NovaTypeException("Awaited expression must be a Future");
  }

  private NovaType typeOfPipe(PipeExpression expression, TypeEnvironment env) {
    NovaType currentType = typeOf(expression.seed(), env);
    for (CallExpression stage : expression.stages()) {
      TypeEnvironment stageEnv = env.createChild();
      stageEnv.defineValue("it", currentType);
      currentType = typeOf(new CallExpression(stage.callee(), stage.arguments()), stageEnv);
    }
    return currentType;
  }

  private NovaType typeOfMatch(MatchExpression expression, TypeEnvironment env) {
    NovaType targetType = typeOf(expression.target(), env);
    NovaType resultType = UnknownType.INSTANCE;
    for (MatchArm arm : expression.arms()) {
      TypeEnvironment scope = env.createChild();
      if (targetType instanceof UserType userType) {
        if (!userType.variants().contains(arm.constructor())) {
          throw new NovaTypeException(
              "Constructor " + arm.constructor() + " not found on type " + userType.name());
        }
      }
      arm.destructured()
          .ifPresent(
              params -> {
                for (Parameter parameter : params) {
                  NovaType paramType =
                      parameter.type().map(this::resolve).orElse(UnknownType.INSTANCE);
                  scope.defineValue(parameter.name(), paramType);
                }
              });
      NovaType armType = typeOf(arm.body(), scope);
      resultType = unify(resultType, armType);
    }
    return resultType;
  }

  private NovaType unify(NovaType left, NovaType right) {
    if (left instanceof UnknownType) {
      return right;
    }
    if (right instanceof UnknownType) {
      return left;
    }
    if (left.equals(right)) {
      return left;
    }
    throw new NovaTypeException("Unable to unify types " + left + " and " + right);
  }

  private void ensureAssignable(NovaType expected, NovaType actual, String message) {
    if (!isAssignable(expected, actual)) {
      throw new NovaTypeException(message + ": expected " + expected + " but found " + actual);
    }
  }

  private boolean isAssignable(NovaType expected, NovaType actual) {
    if (expected instanceof UnknownType || actual instanceof UnknownType) {
      return true;
    }
    if (expected.equals(actual)) {
      return true;
    }
    if (expected instanceof ListType expectedList && actual instanceof ListType actualList) {
      return isAssignable(expectedList.elementType(), actualList.elementType());
    }
    if (expected instanceof FutureType expectedFuture
        && actual instanceof FutureType actualFuture) {
      return isAssignable(expectedFuture.innerType(), actualFuture.innerType());
    }
    if (expected instanceof FunctionType expectedFn && actual instanceof FunctionType actualFn) {
      if (expectedFn.parameters().size() != actualFn.parameters().size()) {
        return false;
      }
      for (int i = 0; i < expectedFn.parameters().size(); i++) {
        if (!isAssignable(expectedFn.parameters().get(i), actualFn.parameters().get(i))) {
          return false;
        }
      }
      return isAssignable(expectedFn.returnType(), actualFn.returnType());
    }
    return false;
  }

  private NovaType resolve(TypeReference reference) {
    return globals
        .lookupType(reference.name())
        .orElseThrow(() -> new NovaTypeException("Unknown type: " + reference.name()));
  }
}
