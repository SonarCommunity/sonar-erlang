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

import org.sonar.squidbridge.api.SourceCode;

import org.sonar.squidbridge.api.SourceFile;

import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public class TestHelper {

  static AstScanner<LexerlessGrammar> scanFiles(List<File> files) {
    AstScanner<LexerlessGrammar> scanner = ErlangAstScanner.create(Charset.forName("UTF-8"));
    scanner.scanFiles(files);
    return scanner;
  }

  public static SourceFile scanSingleFile(File file) {
    AstScanner<LexerlessGrammar> scanner = scanFiles(ImmutableList.of(file));
    return getSourceFile(scanner);
  }

  static SourceFile getSourceFile(AstScanner<LexerlessGrammar> scanner) {
    Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
    return (SourceFile) sources.iterator().next();
  }

}
