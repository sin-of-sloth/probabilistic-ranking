import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.ScoreDoc;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class IndexUtils {
    public void writeScoreDocToOutput(StoredFields storedFields, ScoreDoc[] hits,
                                      String queryNum, Writer outputWriter, Integer start, Integer end) throws IOException {
        for(int i = start; i < end; i++) {
            Document doc = storedFields.document(hits[i].doc);
            outputWriter.write(queryNum + "\t\t"
                    + "Q0" + "\t\t"
                    + doc.get("docID") + "\t\t"
                    + (i + 1) + "\t\t"
                    + hits[i].score + "\t\t"
                    + "alal25" + '\n'
            );
        }
    }

    public void writeDocMapToOutput(Map<Integer, Double> docScores, Map<Integer, String> docIDs,
                                    String queryNum, Writer outputWriter) throws IOException {
        int i = 1;
        for(Map.Entry<Integer, Double> entry : docScores.entrySet()) {
            outputWriter.write(queryNum + "\t\t"
                    + "Q0" + "\t\t"
                    + docIDs.get(entry.getKey()) + "\t\t"
                    + i + "\t\t"
                    + entry.getValue() + "\t\t"
                    + "alal25" + '\n'
            );
            i += 1;
        }
    }

    public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, String order) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        if(order.equals("reverse")) {
            list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        } else {
            list.sort(Map.Entry.comparingByValue());
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public String streamToString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines().collect(Collectors.joining("\n"));
    }

    /**
     * Given a string, removes stop words and removes words having less than 3 characters
     */
    public String stringPreProcessor(String text) {
        List<String> stopwords = loadStopWords();
        // Remove stop words
        String result = removeBlacklistedWords(text, stopwords);
        // Remove words less than 3 characters long
//        result =  result.replaceAll("\\b\\w{1,2}\\b\\s?", "");
        return result;
    }

    public String removeBlacklistedWords(String text, List<String> blacklist) {
        String blacklistRegex = blacklist.stream()
                .collect(Collectors.joining("|", "\\b(", ")\\b\\s?"));
        return text.toLowerCase().replaceAll(blacklistRegex, "");
    }

    public List<String> loadStopWords() {
        return List.of("a", "about", "above", "according", "across", "after", "afterwards", "again", "against", "albeit", "all", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anywhere", "apart", "are", "around", "as", "at", "av", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "both", "but", "by", "can", "cannot", "canst", "certain", "cf", "choose", "contrariwise", "cos", "could", "cu", "day", "de", "do", "does", "doesn't", "doing", "dost", "doth", "double", "down", "dual", "during", "each", "either", "else", "elsewhere", "enough", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "except", "excepted", "excepting", "exception", "exclude", "excluding", "exclusive", "far", "farther", "farthest", "few", "ff", "first", "for", "formerly", "forth", "forward", "from", "front", "further", "furthermore", "furthest", "get", "go", "had", "halves", "hardly", "has", "hast", "hath", "have", "he", "hence", "henceforth", "her", "here", "hereabouts", "hereafter", "hereby", "herein", "hereto", "hereupon", "hers", "herself", "him", "himself", "hindmost", "his", "hither", "hitherto", "how", "however", "howsoever", "i", "ie", "if", "in", "inasmuch", "inc", "include", "included", "including", "indeed", "indoors", "inside", "insomuch", "instead", "into", "inward", "inwards", "is", "it", "its", "itself", "just", "kind", "kg", "km", "last", "latter", "latterly", "less", "lest", "let", "like", "little", "ltd", "many", "may", "maybe", "me", "meantime", "meanwhile", "might", "moreover", "most", "mostly", "more", "mr", "mrs", "ms", "much", "must", "my", "myself", "namely", "need", "neither", "never", "nevertheless", "next", "no", "nobody", "none", "nonetheless", "noone", "nope", "nor", "not", "nothing", "notwithstanding", "now", "nowadays", "nowhere", "of", "off", "often", "ok", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "own", "per", "perhaps", "plenty", "provide", "quite", "rather", "really", "round", "said", "sake", "same", "sang", "save", "saw", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "seldom", "selves", "sent", "several", "shalt", "she", "should", "shown", "sideways", "since", "slept", "slew", "slung", "slunk", "smote", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "spake", "spat", "spoke", "spoken", "sprang", "sprung", "stave", "staves", "still", "such", "supposing", "than", "that", "the", "thee", "their", "them", "themselves", "then", "thence", "thenceforth", "there", "thereabout", "thereabouts", "thereafter", "thereby", "therefore", "therein", "thereof", "thereon", "thereto", "thereupon", "these", "they", "this", "those", "thou", "though", "thrice", "through", "throughout", "thru", "thus", "thy", "thyself", "till", "to", "together", "too", "toward", "towards", "ugh", "unable", "under", "underneath", "unless", "unlike", "until", "up", "upon", "upward", "upwards", "us", "use", "used", "using", "very", "via", "vs", "want", "was", "we", "week", "well", "were", "what", "whatever", "whatsoever", "when", "whence", "whenever", "whensoever", "where", "whereabouts", "whereafter", "whereas", "whereat", "whereby", "wherefore", "wherefrom", "wherein", "whereinto", "whereof", "whereon", "wheresoever", "whereto", "whereunto", "whereupon", "wherever", "wherewith", "whether", "whew", "which", "whichever", "whichsoever", "while", "whilst", "whither", "who", "whoa", "whoever", "whole", "whom", "whomever", "whomsoever", "whose", "whosoever", "why", "will", "wilt", "with", "within", "without", "worse", "worst", "would", "wow", "ye", "yet", "year", "yippee", "you", "your", "yours", "yourself", "yourselves");
    }
}
