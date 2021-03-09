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

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.api.utils.MessageException;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(key = "LineLength", priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.REPOSITORY_NAME, priority = Priority.MAJOR)
@SqaleConstantRemediation("1min")
public class LineLengthCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static final int DEFAULT_MAXIMUM_LINE_LENHGTH = 100;

  private int lastIncorrectLine;

  @RuleProperty(key = "maximumLineLength", defaultValue = "" + DEFAULT_MAXIMUM_LINE_LENHGTH)
  public int maximumLineLength = DEFAULT_MAXIMUM_LINE_LENHGTH;

  @Override
  public void init() {
    if (maximumLineLength <= 0) {
      throw MessageException.of(
              "[AbstractLineLengthCheck] The maximal line length must be set to a value greater than 0 ("
                      + maximumLineLength + " given).");
    }
  }

  @Override
  public void visitFile(AstNode astNode) {
    lastIncorrectLine = -1;
  }

  @Override
  public void visitToken(Token token) {
    if (!token.isGeneratedCode() && lastIncorrectLine != token.getLine()) {
      int incorrectLine = checkLine(token);

      if (incorrectLine > -1) {
        lastIncorrectLine = token.getLine();
        getContext().createLineViolation(this,
                "The line length is greater than {0,number,integer} authorized.",
                incorrectLine, maximumLineLength);
      }
    }
  }

  private int checkLine(Token token) {
    int lineLength = token.getColumn() + token.getValue().length();
    if (lineLength > maximumLineLength) {
      return token.getLine();
    } else if (!token.getTrivia().isEmpty()) {
      for (Trivia trivia : token.getTrivia()) {
        if (trivia.isComment()
                && trivia.getToken().getColumn() + trivia.getToken().getValue().length() > maximumLineLength) {
          return trivia.getToken().getLine();
        }
      }
    }
    return -1;
  }

}
