import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class IndexFiles {
//    private IndexFiles() {}

    /** Index all text files under a directory. */
    public static void main(String[] args) throws Exception {
        String usage = "IndexFiles [INDEX_PATH] [DOCS_PATH]";
        String indexPath;
        String docsPath;
        if(args.length < 2) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        indexPath = args[0];
        docsPath = args[1];

        final Path docDir = Paths.get(docsPath);
        if(!Files.isReadable(docDir)) {
            System.err.println("DOC_PATH '" + docDir.toAbsolutePath() + "' does not exist or is not readable");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (e.g. add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     **/
    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if(Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        documentParser(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index unreadable files
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            documentParser(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    /** Parses a single document, and indexes documents within it */
    static void documentParser(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            // Read through the document to find individual documents.
            // Boundaries are marked by <DOC> and </DOC>
            String line;
            Integer docNo = 0;
            StringBuilder docData = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            while((line = bufferedReader.readLine()) != null) {
                if(line.equals("<DOC>")) {
                    docNo++;
                    docData.delete(0, docData.length());
                } else if(line.equals("</DOC>")) {
                    indexDoc(writer, file, lastModified, docData.toString(), docNo);
                } else {
                    docData.append(line);
                    docData.append('\n');
                }
            }
        }
    }

    /** Indexes a single document */
    static void indexDoc(IndexWriter writer, Path file, long lastModified,
                         String docContents, Integer docNo) throws IOException {
        // make a new, empty document
        Document doc = new Document();

        // Add the path of the file as a field named "path".  Use a
        // field that is indexed (i.e. searchable), but don't tokenize
        // the field into separate words and don't index term frequency
        // or positional information:
        Field pathField = new StringField("path", file.toString(), Field.Store.YES);
        doc.add(pathField);

        // Add the last modified date of the file a field named "modified".
        // Use a LongPoint that is indexed (i.e. efficiently filterable with
        // PointRangeQuery).  This indexes to millisecond resolution, which
        // is often too fine.  You could instead create a number based on
        // year/month/day/hour/minutes/seconds, down the resolution you require.
        // For example the long value 2011021714 would mean
        // February 17, 2011, 2-3 PM.
        doc.add(new LongPoint("modified", lastModified));

        // Use JSoup to parse the tags of the document
        org.jsoup.nodes.Document soupDoc = Jsoup.parse(docContents);
        String docID = "";
        String docText = "";
        // Add the DOC NO of the document as docID
        for(Element element : soupDoc.getElementsByTag("DOCNO")) {
            docID = element.text().strip();
        }
        Field docIDField = new StringField("docID", docID, Field.Store.YES);
        doc.add(docIDField);
        // Create an InputStream from the document contents.
        // The document contents are within <TEXT></TEXT> tags
        for(Element element : soupDoc.getElementsByTag("TEXT")) {
            docText = element.text();
            docText = docText.replaceAll("[^a-zA-Z0-9\\s]", "");
        }
        InputStream stream = new ByteArrayInputStream(docText.getBytes(Charset.defaultCharset()));

        // Add the contents of the file to a field named "contents".  Specify a Reader,
        // so that the text of the file is tokenized and indexed, but not stored.
        // Note that FileReader expects the file to be in UTF-8 encoding.
        // If that's not the case searching for special characters will fail.
        IndexUtils utilsObj = new IndexUtils();
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStoreTermVectors(true);
        fieldType.setStored(true);
        Field textField = new Field("contents", utilsObj.streamToString(stream), fieldType);
        doc.add(textField);

        if(writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            // new index, so we just add the document
            System.out.println("adding document " + docNo + " from " + file);
            writer.addDocument(doc);
        } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            System.out.println("updating " + docNo + " from " + file);
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
    }
}
