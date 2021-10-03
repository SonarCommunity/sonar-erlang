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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.erlang.parser.ErlangGrammarImpl;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(key = "IsTailRecursive", priority = Priority.MAJOR, tags = "bug")
@BelongsToProfile(title = CheckList.REPOSITORY_NAME, priority = Priority.MAJOR)
@SqaleConstantRemediation("1h")
public class IsTailRecursiveCheck extends SquidCheck<LexerlessGrammar> {

  private String actualArity;
  private String actualModule;
  private int lastClauseLine;

  @Override
  public void init() {
    subscribeTo(ErlangGrammarImpl.callExpression, ErlangGrammarImpl.functionDeclaration);
  }

  @Override
  public void visitFile(AstNode astNode) {
    if (astNode == null) {
      // file wasn't parsed
      return;
    }
    actualArity = "";
    actualModule = astNode.getFirstDescendant(ErlangGrammarImpl.moduleAttr)
      .getFirstChild(ErlangGrammarImpl.atom).getTokenOriginalValue();
    lastClauseLine = 0;
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.getType().equals(ErlangGrammarImpl.functionDeclaration)) {
      actualArity = getArity(node.getFirstChild(ErlangGrammarImpl.functionClause));
    } else if (node.getType().equals(ErlangGrammarImpl.callExpression)
      /*
      * Not in a module attribute
      */
      && node.getFirstAncestor(ErlangGrammarImpl.defineAttr) == null
      /*
       * Recursive call
       */
      && getArityFromCall(node).equals(actualArity)
      /*
       * where we have not record a non tail recursive call so far
       */
      && node.getFirstAncestor(ErlangGrammarImpl.functionClause).getTokenLine() != lastClauseLine) {

      /*
       * Not a standalone statement
       */
      if (!node.getParent().getType().equals(ErlangGrammarImpl.expression)
        || node.getParent().getType().equals(ErlangGrammarImpl.expression) && !node.getParent().getParent().getType().equals(ErlangGrammarImpl.expressionStatement)) {
        getContext().createLineViolation(this, "Function is not tail recursive.", node);
        lastClauseLine = node.getFirstAncestor(ErlangGrammarImpl.functionClause).getTokenLine();
        return;
      }

      /*
       * Not last call
       */
      if (!checkIsLastStatement(node.getFirstAncestor(ErlangGrammarImpl.statement))) {
        getContext().createLineViolation(this, "Function is not tail recursive.", node);
        lastClauseLine = node.getFirstAncestor(ErlangGrammarImpl.functionClause).getTokenLine();
      }

    }
  }

  private boolean checkIsLastStatement(AstNode node) {
    if (node == null) {
      return true;
    }
    AstNode sibling = node.getNextSibling();
    if (sibling != null) {
      return false;
    }
    return checkIsLastStatement(node.getFirstAncestor(ErlangGrammarImpl.statement));
  }

  private String getArityFromCall(AstNode ast) {
    /*
     * This method exists in squid/BranchesOfRecursion
     */
    // It has a colon, so it is a module:function call
    if (ast.hasDirectChildren(ErlangGrammarImpl.colon)) {
      AstNode firstCallMemberAstNode = ast.getFirstChild(ErlangGrammarImpl.callExpressionFirstMember);
      AstNode secondCallMemberAstNode = ast.getLastChild(ErlangGrammarImpl.callExpressionSecondMember);

      if (actualModule.equals(firstCallMemberAstNode.getTokenOriginalValue())) {
        return secondCallMemberAstNode.getTokenOriginalValue() + "/" + getNumOfArgs(ast.getFirstChild(ErlangGrammarImpl.arguments));
      }
      // FIXME This seems to use AstNode.toString(), which is likely not intended
      return firstCallMemberAstNode.getFirstChild() + ":" + secondCallMemberAstNode.getTokenOriginalValue() + "/" + getNumOfArgs(ast.getFirstChild(ErlangGrammarImpl.arguments));
    } else {
      try {
        AstNode secondCallMemberAstNode = ast.getLastChild(ErlangGrammarImpl.callExpressionSecondMember);

        return secondCallMemberAstNode.getFirstChild(ErlangGrammarImpl.primaryExpression).getFirstChild(ErlangGrammarImpl.literal).getTokenOriginalValue() + "/"
          + getNumOfArgs(ast.getFirstChild(ErlangGrammarImpl.arguments));
      } catch (Exception e) {
        // If we reach this part it means we are in call where the function is a return value of another function:
        // like: (Fun2())(1)
        return "*" + getNumOfArgs(ast.getFirstChild(ErlangGrammarImpl.arguments));
      }
    }
  }

  private String getArity(AstNode ast) {
    AstNode args = ast.getFirstChild(ErlangGrammarImpl.clauseHead)
      .getFirstChild(ErlangGrammarImpl.funcDecl).getFirstChild(
        ErlangGrammarImpl.arguments);
    return ast.getTokenOriginalValue() + "/" + getNumOfArgs(args);
  }

  private String getNumOfArgs(AstNode args) {
    int num = args.getNumberOfChildren() > 3 ? args.getChildren(
      ErlangGrammarImpl.comma).size() + 1 : args.getNumberOfChildren() - 2;
    return String.valueOf(num);
  }

}
