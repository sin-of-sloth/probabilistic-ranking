import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.util.SortedMap;

public class ProbabilisticRanker {

    public static void main(String[] args) throws Exception {
        // Parse arguments and throw errors if needed
        String usage = """
                ProbabilisticRanker [RANKING_METHOD] [INDEX_PATH] [QUERIES_PATH] [OUTPUT_FILE_PATH]

                RANKING_METHOD can be one of <BM25 / LM / RM1 / RM3>""";
        String rankingMethod;
        String indexPath;
        String queriesPath;
        String outputFile;
        String contentField = "contents";

        if(args.length < 4) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        rankingMethod = args[0];
        indexPath = args[1];
        queriesPath = args[2];
        outputFile = args[3];

        /*
          Build queries from QUERIES_PATH
         */
        QueryBuilder queryBuilder = new QueryBuilder();
        SortedMap<Integer, String> queries = queryBuilder.buildQueries(queriesPath);

        /*
          For each query, get the results and write to the output file
         */
        // Create new output file writer, overwrite if file exists
        Writer outputWriter = new FileWriter(outputFile, false);

        SearchFiles searcher = new SearchFiles();
        // For each topic, get result
        for(Integer queryNum: queries.keySet()) {
            System.out.println("Query: " + queryNum);

            // Create the index searcher and query and get initial results
            String queryString = queries.get(queryNum);
            IndexReader indexReader = searcher.createIndexReader(indexPath);
            IndexSearcher indexSearcher = searcher.createSearcher(indexReader, rankingMethod);
            Query query = searcher.createQuery(contentField, queryString);
            TopDocs results  = searcher.searchIndex(indexSearcher, query);
            ScoreDoc[] hits = results.scoreDocs;

            // Loop through the results and write to output
            int numTotalHits = Math.toIntExact(results.totalHits.value);
            int hitsNeeded = 1000;
            int start = 0;
            int end = Math.min(numTotalHits, hitsNeeded);
            IndexUtils utilObj = new IndexUtils();
            StoredFields storedFields = indexSearcher.storedFields();

            // If BM25 / LM, can directly write to output file.
            // Else re rank the documents
            if(rankingMethod.equals("BM25") || rankingMethod.equals("LM")) {
                utilObj.writeScoreDocToOutput(storedFields, hits,
                        String.valueOf(queryNum), outputWriter, start, end);
            } else if(rankingMethod.equals("RM1")) {
                RM1 rm1Obj = new RM1();
                Map<Integer, Double> docScores = rm1Obj.reRank(indexReader, hits, contentField, queryString, end);
                Map<Integer, String> docIDs = rm1Obj.getDocIDs(storedFields, hits, end);
                utilObj.writeDocMapToOutput(docScores, docIDs, String.valueOf(queryNum), outputWriter);
                System.out.println();
            } else {
                RM3 rm3Obj = new RM3();
                Map<Integer, Double> docScores = rm3Obj.reRank(indexReader, hits, contentField, queryString, end);
                Map<Integer, String> docIDs = rm3Obj.getDocIDs(storedFields, hits, end);
                utilObj.writeDocMapToOutput(docScores, docIDs, String.valueOf(queryNum), outputWriter);
                System.out.println();
            }
        }

        // Close the writer
        outputWriter.close();
    }

}
