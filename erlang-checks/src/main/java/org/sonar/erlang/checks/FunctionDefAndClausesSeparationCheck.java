/*
 * SonarQube Erlang Plugin
 * Copyright © 2012-2018 Tamas Kende <kende.tamas@gmail.com>
 * Copyright © 2018 Denes Hegedus (Cursor Insight Ltd.) <hegedenes@cursorinsight.com>
 * Copyright © 2020 Andris Raugulis <moo@arthepsy.eu>
 * Copyright © 2021 Daniils Petrovs <dpetrovs@evolution.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonar.erlang.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.erlang.parser.ErlangGrammarImpl;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(key = "FunctionDefAndClausesSeparation", priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.REPOSITORY_NAME, priority = Priority.MAJOR)
@SqaleConstantRemediation("1min")
public class FunctionDefAndClausesSeparationCheck extends SquidCheck<LexerlessGrammar> {

  @RuleProperty(key = "allowedBlankLinesBetweenClauses", defaultValue = "0")
  public int allowedBlankLinesBetweenClauses = 0;

  @RuleProperty(key = "allowedBlankLinesBetweenDefinitions", defaultValue = "1")
  public int allowedBlankLinesBetweenDefinitions = 1;

  private AstNode previousDefinition;

  @Override
  public void init() {
    subscribeTo(ErlangGrammarImpl.functionDeclaration);
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    previousDefinition = null;
  }

  @Override
  public void visitNode(AstNode ast) {
    if (!ast.getToken().isGeneratedCode()) {
      /**
       * Check the definition first
       */
      if (ast.getType().equals(ErlangGrammarImpl.functionDeclaration)) {
        if (previousDefinition == null) {
          previousDefinition = ast;
        } else {
          check(ast, previousDefinition, allowedBlankLinesBetweenDefinitions);
          previousDefinition = ast;
        }
      }
      /**
       * Check the clauses
       */
      if (ast.getChildren(ErlangGrammarImpl.functionClause).size() > 1) {
        List<AstNode> funcClauses = ast
          .getChildren(ErlangGrammarImpl.functionClause);
        Iterator<AstNode> clauses = funcClauses.iterator();
        AstNode previousClause = clauses.next();
        while (clauses.hasNext()) {
          AstNode actClause = clauses.next();
          check(actClause, previousClause, allowedBlankLinesBetweenClauses);
          previousClause = actClause;

        }
      }
    }
  }

  private void check(AstNode ast, AstNode previous, int threshold) {
    if (diff(ast.getTokenLine(), previous.getLastToken().getLine(), threshold)) {
      boolean hasTrivias = ast.getToken().hasTrivia();
      if ((hasTrivias && checkTrivias(ast.getToken(), previous.getToken(), threshold)) || !hasTrivias) {
        if (ast.getTokenLine() - previous.getLastToken().getLine() - 1 >= 0) {
          if (!ast.getPreviousAstNode().equals(previous)) {
            check(ast, ast.getPreviousAstNode(), 0);
          } else {
            getContext().createLineViolation(this,
              "The line has {0} precending blank line and it should be: {1}.",
              ast.getTokenLine(), ast.getTokenLine() - previous.getLastToken().getLine() - 1,
              threshold);
          }
        }
      }
    }
  }

  private boolean diff(int a, int b, int threshold) {
    if (a - b - 1 != threshold) {
      return true;
    }
    return false;
  }

  private boolean checkTrivias(Token token, Token token2, int threshold) {
    int actLine = token2.getLine();
    for (Trivia trivia : token.getTrivia()) {
      if (actLine - trivia.getToken().getLine() - 1 > threshold) {
        return true;
      }
      actLine = trivia.getToken().getLine();
    }
    return false;
  }

}
