package com.github.cybellereaper.noblepets.nova.runtime;

import com.github.cybellereaper.noblepets.nova.ast.AsyncExpression;
import com.github.cybellereaper.noblepets.nova.ast.AwaitExpression;
import com.github.cybellereaper.noblepets.nova.ast.BlockExpression;
import com.github.cybellereaper.noblepets.nova.ast.CallExpression;
import com.github.cybellereaper.noblepets.nova.ast.FunctionDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.IdentifierExpression;
import com.github.cybellereaper.noblepets.nova.ast.LetDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.ModuleDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.NovaProgram;
import com.github.cybellereaper.noblepets.nova.ast.NumberLiteralExpression;
import com.github.cybellereaper.noblepets.nova.ast.Parameter;
import com.github.cybellereaper.noblepets.nova.ast.StringLiteralExpression;
import com.github.cybellereaper.noblepets.nova.ast.TypeReference;
import com.github.cybellereaper.noblepets.nova.type.FunctionType;
import com.github.cybellereaper.noblepets.nova.type.PrimitiveType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaRuntimeTest {
  @Test
  void loadsProgramAndEvaluatesDeclarations() {
    FunctionDeclaration function =
        new FunctionDeclaration(
            "greet",
            List.of(Parameter.typed("name", new TypeReference("String"))),
            Optional.of(new TypeReference("String")),
            new IdentifierExpression("name"));
    LetDeclaration letDeclaration =
        new LetDeclaration(
            "message",
            Optional.of(new TypeReference("String")),
            new CallExpression(
                new IdentifierExpression("greet"), List.of(new StringLiteralExpression("Nova"))));
    NovaProgram program =
        new NovaProgram(new ModuleDeclaration(List.of("sample")), List.of(function, letDeclaration));

    try (NovaRuntime runtime = NovaRuntime.createDefault()) {
      runtime.load(program);
      NovaValue value = runtime.valueEnvironment().lookup("message").orElseThrow();
      NovaStringValue stringValue = assertInstanceOf(NovaStringValue.class, value);
      assertEquals("Nova", stringValue.value());
    }
  }

  @Test
  void awaitExpressionJoinsFuture() {
    try (NovaRuntime runtime = NovaRuntime.createDefault()) {
      NovaValue value =
          runtime.evaluate(
              new AwaitExpression(
                  new AsyncExpression(
                      new BlockExpression(
                          List.of(new NumberLiteralExpression(BigDecimal.valueOf(42)))))));
      NovaNumberValue numberValue = assertInstanceOf(NovaNumberValue.class, value);
      assertEquals(BigDecimal.valueOf(42), numberValue.value());
    }
  }

  @Test
  void hostFunctionsCanBeRegisteredWithTypes() {
    try (NovaRuntime runtime = NovaRuntime.createDefault()) {
      runtime.registerHostFunction(
          "identity",
          args -> args.getFirst(),
          FunctionType.sync(List.of(PrimitiveType.STRING), PrimitiveType.STRING));
      NovaValue result =
          runtime.evaluate(
              new CallExpression(
                  new IdentifierExpression("identity"),
                  List.of(new StringLiteralExpression("hello"))));
      NovaStringValue stringValue = assertInstanceOf(NovaStringValue.class, result);
      assertEquals("hello", stringValue.value());
      assertTrue(runtime.typeEnvironment().lookupValue("identity").isPresent());
    }
  }
}
