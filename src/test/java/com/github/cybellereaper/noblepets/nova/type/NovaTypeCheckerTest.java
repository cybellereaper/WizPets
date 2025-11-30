package com.github.cybellereaper.noblepets.nova.type;

import com.github.cybellereaper.noblepets.nova.ast.LetDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.ModuleDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.NovaProgram;
import com.github.cybellereaper.noblepets.nova.ast.NumberLiteralExpression;
import com.github.cybellereaper.noblepets.nova.ast.Parameter;
import com.github.cybellereaper.noblepets.nova.ast.FunctionDeclaration;
import com.github.cybellereaper.noblepets.nova.ast.IdentifierExpression;
import com.github.cybellereaper.noblepets.nova.ast.StringLiteralExpression;
import com.github.cybellereaper.noblepets.nova.ast.TypeReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NovaTypeCheckerTest {
  @Test
  void letBindingHonorsAnnotation() {
    NovaProgram program =
        new NovaProgram(
            new ModuleDeclaration(List.of("sample")),
            List.of(
                new LetDeclaration(
                    "answer",
                    Optional.of(new TypeReference("Number")),
                    new NumberLiteralExpression(BigDecimal.ONE))));
    TypeChecker checker = new TypeChecker(new TypeEnvironment());
    assertDoesNotThrow(() -> checker.checkProgram(program));
  }

  @Test
  void letBindingRejectsMismatch() {
    NovaProgram program =
        new NovaProgram(
            new ModuleDeclaration(List.of("sample")),
            List.of(
                new LetDeclaration(
                    "answer",
                    Optional.of(new TypeReference("String")),
                    new NumberLiteralExpression(BigDecimal.ONE))));
    TypeChecker checker = new TypeChecker(new TypeEnvironment());
    Executable check = () -> checker.checkProgram(program);
    assertThrows(NovaTypeException.class, check);
  }

  @Test
  void functionReturnMustMatchAnnotation() {
    FunctionDeclaration function =
        new FunctionDeclaration(
            "identity",
            List.of(Parameter.typed("value", new TypeReference("String"))),
            Optional.of(new TypeReference("String")),
            new IdentifierExpression("value"));
    NovaProgram program =
        new NovaProgram(new ModuleDeclaration(List.of("sample")), List.of(function));
    TypeChecker checker = new TypeChecker(new TypeEnvironment());
    assertDoesNotThrow(() -> checker.checkProgram(program));

    FunctionDeclaration badFunction =
        new FunctionDeclaration(
            "broken",
            List.of(Parameter.typed("value", new TypeReference("String"))),
            Optional.of(new TypeReference("Number")),
            new StringLiteralExpression("oops"));
    NovaProgram badProgram =
        new NovaProgram(new ModuleDeclaration(List.of("sample")), List.of(badFunction));
    Executable check = () -> checker.checkProgram(badProgram);
    assertThrows(NovaTypeException.class, check);
  }
}
