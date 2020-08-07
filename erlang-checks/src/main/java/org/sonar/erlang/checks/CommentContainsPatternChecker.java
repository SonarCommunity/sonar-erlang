/*
 * SonarQube Erlang Plugin
 * Copyright © 2012-2018 Tamas Kende <kende.tamas@gmail.com>
 * Copyright © 2018 Denes Hegedus (Cursor Insight Ltd.) <hegedenes@cursorinsight.com>
 * Copyright © 2020 Andris Raugulis <moo@arthepsy.eu>
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

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.squidbridge.checks.SquidCheck;

public class CommentContainsPatternChecker {

  private final SquidCheck<?> check;
  private final List<String> patterns;
  private final String message;

  public CommentContainsPatternChecker(SquidCheck<?> check, String pattern, String message) {
    this.check = check;
    patterns = Arrays.asList(pattern.split("\\|"));
    this.message = message;
  }

  public void visitToken(Token token) {
    for (Trivia trivia : token.getTrivia()) {
      String comment = trivia.getToken().getOriginalValue();
      for (String pattern : patterns) {
        if (StringUtils.containsIgnoreCase(comment, pattern)) {
          String[] lines = comment.split("\r\n?|\n");
          for (int i = 0; i < lines.length; i++) {
            if (StringUtils.containsIgnoreCase(lines[i], pattern)) {
              check.getContext().createLineViolation(check, message, trivia.getToken().getLine() + i);
            }
          }
        }
      }
    }
  }

}
