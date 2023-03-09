import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;

public class SearchFiles {
    public TopDocs searchIndex(IndexSearcher indexSearcher, Query query) throws Exception {
        TotalHitCountCollector collector = new TotalHitCountCollector();
        indexSearcher.search(query, collector);
        return indexSearcher.search(query, Math.max(1, collector.getTotalHits()));
    }

    public IndexReader createIndexReader(String indexPath) throws IOException {
        return DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
    }

    public IndexSearcher createSearcher(IndexReader indexReader, String rankingMethod) {
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        if(rankingMethod.equals("BM25")) {
            indexSearcher.setSimilarity(new BM25Similarity());
        } else {
            indexSearcher.setSimilarity(new LMDirichletSimilarity());
        }
        return indexSearcher;
    }

    public Query createQuery(String field, String queryString) throws ParseException {
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser(field, analyzer);
        return queryParser.parse(queryString);
    }
}
