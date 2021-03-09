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
package org.sonar.erlang;

import com.google.common.collect.ImmutableList;

import org.sonar.squidbridge.AstScanner;
import org.junit.Test;
import org.sonar.erlang.api.ErlangMetric;

import org.sonar.squidbridge.api.SourceClass;

import org.sonar.squidbridge.api.SourceCode;

import org.sonar.squidbridge.api.SourceFile;

import org.sonar.squidbridge.api.SourceProject;

import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class ErlangAstScannerTest {

  @Test
  public void files() {
    AstScanner<LexerlessGrammar> scanner = TestHelper.scanFiles(ImmutableList.of(
      new File("src/test/resources/metrics/lines.erl"),
      new File("src/test/resources/metrics/lines_of_code.erl")));
    SourceProject project = (SourceProject) scanner.getIndex().search(
      new QueryByType(SourceProject.class)).iterator().next();
    assertThat(project.getInt(ErlangMetric.FILES)).isEqualTo(2);
  }

  @Test
  public void comments() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/functions.erl"));
    assertThat(file.getInt(ErlangMetric.COMMENT_LINES)).isEqualTo(5);
    assertThat(file.getNoSonarTagLines()).contains(38);
    assertThat(file.getNoSonarTagLines().size()).isEqualTo(1);
  }

  @Test
  public void modules() {
    AstScanner<LexerlessGrammar> scanner = TestHelper.scanFiles(ImmutableList.of(
      new File("src/test/resources/metrics/functions.erl")));
    SourceClass module = (SourceClass) scanner.getIndex().search(
      new QueryByType(SourceClass.class)).iterator().next();
    assertThat(module.getKey()).isEqualTo("functions:1");
    assertThat(TestHelper.getSourceFile(scanner).getInt(ErlangMetric.MODULES)).isEqualTo(1);
  }

  @Test
  public void lines() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/lines.erl"));
    assertThat(file.getInt(ErlangMetric.LINES)).isEqualTo(5);
  }

  @Test
  public void publicAPIs() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/functions.erl"));
    assertThat(file.getInt(ErlangMetric.PUBLIC_API)).isEqualTo(7);
    assertThat(file.getInt(ErlangMetric.PUBLIC_DOC_API)).isEqualTo(4);
    assertThat(file.getInt(ErlangMetric.PUBLIC_DOCUMENTED_API_DENSITY)).isEqualTo((4 / 7));
  }

  @Test
  public void lines_of_code() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/lines_of_code.erl"));
    assertThat(file.getInt(ErlangMetric.LINES_OF_CODE)).isEqualTo(3);
  }

  @Test
  public void lines_of_code2() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/lines_of_code2.erl"));
    assertThat(file.getInt(ErlangMetric.LINES_OF_CODE)).isEqualTo(14);
  }

  @Test
  public void functions2() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/lines_of_code2.erl"));
    assertThat(file.getInt(ErlangMetric.FUNCTIONS)).isEqualTo(2);
  }

  @Test
  public void statements() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/statements.erl"));
    assertThat(file.getInt(ErlangMetric.STATEMENTS)).isEqualTo(20);
  }

  @Test
  public void functions() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/functions.erl"));
    assertThat(file.getInt(ErlangMetric.FUNCTIONS)).isEqualTo(7);
  }

  @Test
  public void complexity() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/complexity.erl"));
    assertThat(file.getInt(ErlangMetric.COMPLEXITY)).isEqualTo(10);
  }

  @Test
  public void numOfFunExpr() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/funexpressions.erl"));
    assertThat(file.getInt(ErlangMetric.NUM_OF_FUN_EXRP)).isEqualTo(4);
  }

  @Test
  public void numOfFunctionArguments() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/funargs.erl"));
    assertThat(file.getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(21);
    Set<SourceCode> children = file.getChildren();
    SourceCode[] classes = children.toArray(new SourceCode[children.size()]);
    SourceCode[] functions = classes[0].getChildren().toArray(new SourceCode[classes[0].getChildren().size()]);
    assertThat(functions[0].getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(0);
    assertThat(functions[1].getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(6);
    assertThat(functions[2].getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(1);
    assertThat(functions[3].getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(14);
    assertThat(
      functions[3].getChildren().toArray(
        new SourceCode[functions[3].getChildren().size()])[0]
        .getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(7);
    assertThat(
      functions[3].getChildren().toArray(
        new SourceCode[functions[3].getChildren().size()])[1]
        .getInt(ErlangMetric.NUM_OF_FUNC_ARGS)).isEqualTo(7);

  }

  @Test
  public void branchesOfRecursion() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/branchesofrecursion.erl"));
    assertThat(file.getInt(ErlangMetric.BRANCHES_OF_RECURSION)).isEqualTo(3);
  }

  @Test
  public void numOfFunctionClauses() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/funargs.erl"));
    assertThat(file.getInt(ErlangMetric.NUM_OF_FUN_CLAUSES)).isEqualTo(5);
  }

  @Test
  public void numOfMacros() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/metrics/macros.erl"));
    assertThat(file.getInt(ErlangMetric.NUM_OF_MACROS)).isEqualTo(2);
  }

  @Test
  public void megaco() {
    SourceFile file = TestHelper.scanSingleFile(new File(
      "src/test/resources/megaco_ber_media_gateway_control_v1.erl"));
    assertThat(file.getInt(ErlangMetric.FILES)).isEqualTo(1);
  }

}
