import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RM3 {
    public Map<Integer, Double> reRank(IndexReader indexReader, ScoreDoc[] initialHits,
                                       String contentField, String queryString, Integer nDocs) throws IOException {
        RM1 rm1Obj = new RM1();
        IndexUtils utilObj = new IndexUtils();
        Double lambda = 0.2;

        // Get RM1 scores
        Map<Integer, Double> rm1Scores = rm1Obj.reRank(indexReader, initialHits, contentField, queryString, nDocs);
        Map<Integer, Double> docQueryLikelihoods = rm1Obj.getDocumentQueryLikelihoods(initialHits, nDocs);
        Map<Integer, Double> finalDocScores = new HashMap<>();
        for(Map.Entry<Integer, Double> entry : rm1Scores.entrySet()) {
            Double rm1Score = entry.getValue();
            Double qleScore = docQueryLikelihoods.get(entry.getKey());
            finalDocScores.put(entry.getKey(), ((rm1Score * lambda) + (qleScore * (1.0 - lambda))));
        }
        finalDocScores = utilObj.sortByValue(finalDocScores, "reverse");
        return finalDocScores;
    }

    public Map<Integer, String> getDocIDs(StoredFields storedFields, ScoreDoc[] initialHits, int nDocs) throws IOException {
        Map<Integer, String> docIDs = new HashMap<>();
        for(int i = 0; i < nDocs; i++) {
            Document doc = storedFields.document(initialHits[i].doc);
            docIDs.put(initialHits[i].doc, doc.get("docID"));
        }
        return docIDs;
    }
}
