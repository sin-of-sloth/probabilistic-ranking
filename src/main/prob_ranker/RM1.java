import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

public class RM1 {

    double smoothingValue = 0.5;
    int topKDocs = 50;
    int topNWords = 10;

    /**
     * Re-ranks a set of documents using RM1 method
     */
    public Map<Integer, Double> reRank(IndexReader indexReader, ScoreDoc[] initialHits,
                                       String contentField, String queryString, Integer nDocs) throws IOException {
        Map<Integer, Double> docScores = new HashMap<>();
        IndexUtils utilObj = new IndexUtils();
        TermVectors termVectors = indexReader.termVectors();
        Map<Integer, Terms> docTermVectors = createDocTermVectors(initialHits, nDocs, termVectors, contentField);

        // Remove stop words from query
        queryString = utilObj.stringPreProcessor(queryString.toLowerCase());

        // Get query likelihood per document
        Map<Integer, Double> docQueryLikelihoods = getDocumentQueryLikelihoods(initialHits, nDocs);

        System.out.print("Calculating term weights...");
        // Count the terms in top k docs
        Map<String, Map<Integer, Integer>> termCounts = getDocWiseTermCounts(docTermVectors, topKDocs, initialHits, utilObj.loadStopWords());
        // Get doc wise weights of each term
        Map<String, Map<Integer, Double>> docWiseTermWeights = getDocWiseTermWeights(termCounts, docTermVectors);

        // Get term weights for the unique words
        Map<String, Double> termWeights = getTotalTermWeights(docWiseTermWeights);
        System.out.println("done");

        // Normalize term weights
        double allTermTotalWeight = termWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        termWeights.replaceAll((k, v) -> (v / allTermTotalWeight));

        // Order term weights
        termWeights = utilObj.sortByValue(termWeights, "reverse");

        // Get expanded query
        String expandedQuery = getExpandedQuery(queryString, termWeights, topNWords);

        System.out.println("Expanded Query: " + expandedQuery);

        // Get final doc scores
        docScores = getFinalDocScores(docWiseTermWeights, docQueryLikelihoods, expandedQuery);
        // Order them by descending score
        docScores = utilObj.sortByValue(docScores, "reverse");

        return docScores;
    }

    public Map<String, Map<Integer, Integer>> getDocWiseTermCounts(Map<Integer, Terms> docTermVectors,
                                                                   Integer nDocs, ScoreDoc[] initialHits, List<String> blackList) throws IOException {
        Map<String, Map<Integer, Integer>> docWiseTermCounts = new HashMap<>();
        Map<Integer, Integer> termCounts;
        BytesRef termBytes = new BytesRef();
        String term;

        for(int i = 0; i < nDocs; i++) {
            Terms terms = docTermVectors.get(initialHits[i].doc);
            TermsEnum termsEnum = terms.iterator();
            int termFrequency;
            while((termsEnum.next()) != null) {
                termBytes = termsEnum.term();
                term = termBytes.utf8ToString();
                // If in blacklist, continue
                if(blackList.contains(term)) {
                    continue;
                }

                PostingsEnum pe = termsEnum.postings(null, PostingsEnum.FREQS);
                pe.nextDoc();
                termFrequency = pe.freq();
                if(!docWiseTermCounts.containsKey(term)) {
                    docWiseTermCounts.put(term, new HashMap<>());
                }
                termCounts = docWiseTermCounts.get(term);
                termCounts.put(initialHits[i].doc, termFrequency);
            }
        }
        return docWiseTermCounts;
    }

    public Map<String, Map<Integer, Double>> getDocWiseTermWeights(Map<String, Map<Integer, Integer>> docWiseTermCounts, Map<Integer, Terms> docTermVectors) throws IOException {
        Map<String, Map<Integer, Double>> docWiseTermWeights = new HashMap<>();
        for(Map.Entry<String, Map<Integer, Integer>> countEntry : docWiseTermCounts.entrySet()) {
            String term = countEntry.getKey();
            docWiseTermWeights.put(term, new HashMap<>());
            for(Integer docID : docTermVectors.keySet()) {
                long docLength = docTermVectors.get(docID).getSumTotalTermFreq();
                int termCount = 0;
                if(docWiseTermCounts.get(term).containsKey(docID)) {
                    termCount = docWiseTermCounts.get(term).get(docID);
                }
                Double termLikelihood = (termCount + smoothingValue) / (docLength + (docLength * smoothingValue));
                docWiseTermWeights.get(term).put(docID, termLikelihood);
            }
        }

        return  docWiseTermWeights;
    }

    public SortedMap<String, Double> getTotalTermWeights(Map<String, Map<Integer, Double>> docWiseTermWeights) {
        SortedMap<String, Double> termWeights = new TreeMap<>();
        for(Map.Entry<String, Map<Integer, Double>> countEntry : docWiseTermWeights.entrySet()) {
            String term = countEntry.getKey();
            termWeights.put(term, docWiseTermWeights.get(term).values().stream().mapToDouble(Double::doubleValue).sum());
        }

        return  termWeights;
    }

    public Map<Integer, String> getDocIDs(StoredFields storedFields, ScoreDoc[] initialHits, int nDocs) throws IOException {
        Map<Integer, String> docIDs = new HashMap<>();
        for(int i = 0; i < nDocs; i++) {
            Document doc = storedFields.document(initialHits[i].doc);
            docIDs.put(initialHits[i].doc, doc.get("docID"));
        }
        return docIDs;
    }

    public Map<Integer, Terms> createDocTermVectors(ScoreDoc[] initialHits, Integer nDocs,
                                                   TermVectors termVectors, String contentField) throws IOException {
        Map<Integer, Terms> docTermVectors = new HashMap<>();
        for(int i = 0; i < nDocs; i++) {
            Terms terms = termVectors.get(initialHits[i].doc, contentField);
            docTermVectors.put(initialHits[i].doc, terms);
        }
        return docTermVectors;
    }

    public Map<Integer, Double> getDocumentQueryLikelihoods(ScoreDoc[] initialHits, Integer nDocs) {
        Map<Integer, Double> docQueryLikelihoods = new HashMap<>();

        // Loop through each document and find query likelihoods
        for(int i = 0; i < nDocs; i++) {
            docQueryLikelihoods.put(initialHits[i].doc, (double) initialHits[i].score);
        }

        return docQueryLikelihoods;
    }

    public Map<Integer, Double> getFinalDocScores(Map<String, Map<Integer, Double>> docWiseTermWeights,
                                                  Map<Integer, Double> docQueryLikelihoods, String query) {
        Map<Integer, Double> finalDocScores = new HashMap<>();
        String[] queryTerms = query.split("\\s+");
        for(Map.Entry<Integer, Double> docScore : docQueryLikelihoods.entrySet()) {
            double newDocScore = 0.0;
            int docID = docScore.getKey();
            double docQL = docScore.getValue();
            for(String term : queryTerms) {
                if(docWiseTermWeights.containsKey(term) && docWiseTermWeights.get(term).containsKey(docID)) {
                    double docTermScore = docWiseTermWeights.get(term).get(docID) * docQL;
                    newDocScore += docTermScore;
                }
            }
            finalDocScores.put(docID, newDocScore);
        }
        return finalDocScores;
    }

    public String getExpandedQuery(String queryString, Map<String, Double> termWeights, int topNWords) {
        StringBuilder expandedQuery = new StringBuilder(queryString);
        int n = 0;
        for(Map.Entry<String, Double> entry : termWeights.entrySet()) {
            expandedQuery.append(" ");
            expandedQuery.append(entry.getKey());
            n++;
            if(n == topNWords) break;
        }
        return expandedQuery.toString();
    }
}
