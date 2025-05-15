/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.infra;

import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public class CustemChutneyAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(source);
        List<String> stopWords = List.of("#", "-", "+", "~", "*", "/", "\\");
        CharArraySet stopSet = new CharArraySet(stopWords, true);
        tokenStream = new StopFilter(tokenStream, stopSet);
        return new TokenStreamComponents(source, tokenStream);
    }
}
