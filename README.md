# Probabilistic Ranking

<p>
    <a href="https://docs.aws.amazon.com/corretto/latest/corretto-18-ug/downloads-list.html">
        <img src="https://img.shields.io/static/v1?label=Java&style=flat-square&logo=java&message=corretto-18&color=green" />
    </a>
    <a href="https://lucene.apache.org/core/9_5_0/">
        <img src="https://custom-icon-badges.demolab.com/static/v1?label=Apache Lucene&style=flat-square&logo=apache_lucene_logo&logoWidth=50&message=9.5.0&color=green" />
    </a>
</p>

Done as part of CS572 Information Retrieval instructed by [Dr. Eugene Agichtein](http://www.cs.emory.edu/~eugene/).

Developed and tested on Ubuntu 22.04.2 LTS.

To implement probabilistic ranking with basic smoothing, and with blind relevance feedback. A standard benchmark corpus
is provided for ranking evaluation (subset of TREC ad-hoc track). The mission is to implement the ranking methods
described below by over-riding the built-in ranking models in Lucene.

## Contents
1. [Background Information](#1-background-information)
   1. [Dataset](#11-dataset)
   2. [Ranking Methods](#12-ranking-methods)
2. [Implementation Details](#2-implementation-details)
   1. [Indexing the Data](#21-indexing-the-data)
   2. [Ranking the Data](#22-ranking-the-data)
      1. [Input Queries](#221-input-queries)
      2. [Output Format](#222-output-format)
      3. [Formal Evaluation](#223-formal-evaluation)
3. [Source Code Details](#source-code-details)
   1. [Constants Used](#constants-used)
   2. [File and Method Descriptions](#file-and-method-descriptions)
4. [More Information](#more-information)
5. [References](#references)

## 1 Background Information

### 1.1 Dataset

Dataset used is a subset of the TREC (Text REtrieval Conference) ad-hoc track, [Text Research Collection Volume 4, May
1996 and Text Research Collection Volume 5, April 1997](https://trec.nist.gov/data/docs_eng.html). Document sets used
are `fbis`, `ft`, and `latimes`.

### 1.2 Ranking Methods

- Lucene BM25 (baseline)
- LM-based Similarity (`LMDirichletSimilarity` or `LMJelinekMercerSimilarity`)
- RM1 (Lavrenko & Croft, SIGIR 2001)
- RM3 (Lavrenko & Croft, SIGIR 2001)

Sample implementations of RM1 and RM3 can be found from
[The Lemur Project / Galago](https://sourceforge.net/p/lemur/galago/ci/bed290b74b78bfeba69d1cc24b469459e87fc717/tree/core/src/main/java/org/lemurproject/galago/core/retrieval/prf/).

#### Additional Information

- To implement Lucene BM25, you can use the `BM25Similarity` class:
  https://lucene.apache.org/core/9_5_0/core/org/apache/lucene/search/similarities/BM25Similarity.html
- To implement LM-based Similarity, you can use either of the Lucene implemented APIs below:
  https://lucene.apache.org/core/7_2_1/core/org/apache/lucene/search/similarities/LMDirichletSimilarity.html
  https://lucene.apache.org/core/7_2_1/core/org/apache/lucene/search/similarities/LMJelinekMercerSimilarity.html
- RM1 and RM3 are expansion methods. Therefore, you will need to retrieve the top-K documents first, and then process
  the top retrieved documents to extract the candidate words and estimate the probabilities needed for RM1 and RM3.
  - To do the initial retrieval, you can use one of `LMDirichletSimilarity` or `LMJelinekMercerSimilarity` classes.
  - Do not do a new search with expanded query terms, only re-rank the original retrieved list of documents. You might
    want to use a different value of K for computing the RM1 statistics, and for initial top-K retrieved list. For
    example, estimate probabilities using top-10 or top-50 documents, but retrieved list for re-ranking can be bigger,
    e.g., 100 or 1000 documents.

## 2 Implementation Details

### 2.1 Indexing the Data

Each file in the dataset has multiple documents, and the boundaries are marked by `<DOC>` and `</DOC>`. You can index
the files using
```
java IndexFiles [INDEX_PATH] [DOCS_PATH]
```

### 2.2 Ranking the Data

To rank the data run the program using:

```
java ProbabilisticRanker [RANKING_METHOD] [INDEX_PATH] [QUERIES_PATH] [OUTPUT_FILE_PATH]
```

or run the created jar file, [ProbabilisticRanker.jar](./ProbabilisticRanker.jar):

```
java -jar ProbabilisticRanker.jar [RANKING_METHOD] [INDEX_PATH] [QUERIES_PATH] [OUTPUT_FILE_PATH]
```

`RANKING_METHOD` can be one of `BM25` / `LM` / `RM1` / `RM3`

#### 2.2.1 Input Queries

The input query file is [TREC-7 ad hoc and TREC-8 filtering topics](https://trec.nist.gov/data/topics_eng/index.html).
A copy can be found in [eval/topics.351-400](./eval/topics.351-400).

We need to parse and convert "topics" into queries:
- Each topic is marked by `<top>` and `</top>`
- We use the union of `Title` and `Description` fields as the query for each topic

#### 2.2.2 Output Format

For each topic, return up to 1,000 documents, one result per line, tab-delimited, in following format:

```
topic_id  \t Q0 \t document_id \t rank \t score \t your_id
```

- `topic_id` is the number specified in <num> for each topic in the input query
- `Q0` is a constant
- `document_id` is the value of the corresponding "DocId" for the document in index
- `rank` is the rank of the document for the query
- `score` is the score of the document for the query
- `your_id` is just an identifier for you

Example result:

```
351      Q0      FT944-4100      386      12.699199      alal25
```

#### 2.2.3 Formal Evaluation

An automatic evaluation program from TREC is available in [eval](./eval). The output file should be in the correct
format as specified.

Example:

Assume we entered:
```
java -jar ProbabilisticRanker.jar BM25 "./index" "eval/topics.351-400" "results/BM25results.txt"
```

The program uses BM25 to search the index, retrieves the top 1000 documents for each one of the 50 queries, and stores
the results (concatenated) in `results/BM25results.txt` as follows:

```
351     Q0      FT934-4848      1       28.310038     alal25
351     Q0      FT921-6603      2       26.861269     alal25
351     Q0      FT941-9999      3       26.669258     alal25
...
351     Q0      FT941-7250      999     9.0101385     alal25
351     Q0      FT942-11134     1000    9.006053      alal25    # up to this point the results of the first query
352     Q0      FT943-904       1       21.337904     alal25
352     Q0      FT934-11803     2       16.779995     alal25
352     Q0      LA120290-0163   3       16.102392     alal25
...
400     Q0      LA040690-0150   999     9.383813      alal25
400     Q0      FT934-13571     1000    9.378035      alal25    # query results ends
```

You can evaluate using:
```
eval/trec_eval.linux eval/qrels.trec7 results/BM25results.txt
```

## 3 Source Code Details

The source code can be found under [src/](./src), and maven dependencies in [pom.xml](./pom.xml).

Java application developed with SDK `corretto-18`.

### 3.1 Constants Used

- `smoothingValue`  - `0.5`, alpha value for laplace smoothing
- `k`               - `50`, number of docs from which terms have to be weighted
- `n`               - `10`, number of terms with the largest weights to be added to expanded query
- `lambda`          - `0.5`, lambda value used to score docs using RM3 method

### 3.2 File and Method Descriptions

1. `IndexFiles.java` - used for indexing set of documents

2. `IndexUtils.java` - contains utility functions for the pipeline
   METHODS
    -------
    - `writeScoreDocToOutput()`
      writes the document scores and ranks from a `ScoreDoc[]` type variable to output file
    - `writeDocMapToOutput()`
      writes the document scores and ranks from a map of `<Document ID, Document Score>` to output file
    - `sortByValue()`
      used to sort a map comparing its values
    - `streamToString()`
      converts a stream object to a string
    - `stringPreProcessor()`
      removes stop words from a string
    - `removeBlacklistedWords()`
      removes blacklisted words from a string
    - `loadStopWords()`
      returns a list of stop words; stop words taken from the Galago Project:
      https://sourceforge.net/p/lemur/galago/ci/bed290b74b78bfeba69d1cc24b469459e87fc717/tree/core/src/main/resources/stopwords/rmstop

3. `ProbabilisticRanker.java` - controller for the assignment; parses arguments, does initial retrievals for each
   query based on the ranking method, re ranks them if it's RM1 or RM3, and writes final ranks and scores to output
   ARGUMENTS
    ---------
    ```
    ProbabilisticRanker [RANKING_METHOD] [INDEX_PATH] [QUERIES_PATH] [OUTPUT_FILE_PATH]
   ```
   `RANKING_METHOD` can be one of `BM25` / `LM` / `RM1` / `RM3`

4. `QueryBuilder.java` - creates a map of queries from the query file with query number as key and Title + Description as value
   METHODS
    -------
    - `buildQueries()`
      does above-mentioned task

5. `RM1.java` - Re ranks set of scored documents based on RM1 ranking method
   METHODS
    -------
    - `reRank()`
      re ranks a set of documents and returns a map of `<Document ID, Document Score>`
      Steps done:
      * remove stop words from query
      * get document query likelihoods (taken from `LMDirichletSimilarity` QLEs)
      * calculate term weights of top 50 documents
      * create expanded query using query + top 10 words
      * get new document scores for new expanded query
    - `getDocWiseTermCounts()`
      for each term in the set of top 50 documents, find document wise frequency; ignores stop words and query words
    - `getDocWiseTermWeights()`
      for each term in the set of top 50 documents, find document wise weight
    - `getTotalTermWeights()`
      for each term in the set of top 50 documents, get their total weight (sum of term weight from each doc)
    - `getDocIDs()`
      creates a map of Document IDs to Document names
    - `createDocTermVectors()`
      creates a map of Document IDs to their term vector
    - `getDocumentQueryLikelihoods()`
      creates a map from Document ID to their query likelihoods (taken from `LMDirichletSimilarity` QLEs)
    - `getFinalDocScores()`
      re scores documents based on the term weights and document query likelihoods
    - g`etExpandedQuery()`
      creates the expanded query of old query + top 10 terms (ignoring terms already in query)

6. `RM3.java` - Re ranks a set of documents based on RM3 ranking method
   METHODS
    -------
    - `reRank()`
      re ranks a set of documents and returns a map of `<Document ID, Document Score>`
      Steps done:
      * gets RM1 document ranks
      * generates new scores that are `(lambda * RM1score) + ((1 - lambda) * QLE)`; `lambda` value is `0.2`

7. `SearchFiles.java` - searches the index and returns `ScoreDocs[]` object
   METHODS
    -------
    - `searchIndex()`
      searches the index and returns `ScoreDocs[]` objects
    - `createIndexReader()`
      creates a lucene `IndexReader` for the index
    - `createSearcher()`
      creates a lucene `IndexSearcher` for the index after setting a similarity based on ranking method
    - `createQuery()`
      creates a lucene `Query` object for the query string


## 4 More Information

Results obtained for this program is available in [results](./results).

## 5 References
- Lavrenko, V., & Croft, W. B. (2001). _Relevance based language models_. In Proceedings of the 24th annual international
ACM SIGIR conference on Research and development in information retrieval. SIGIR01: 24th ACM/SIGIR International
Conference on Research and Development in Information Retrieval. ACM. https://doi.org/10.1145/383952.383972
