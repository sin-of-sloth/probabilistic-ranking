import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryBuilder {
    public SortedMap buildQueries(String queryPath) {
        final Path queryFilePath = Paths.get(queryPath);
        if(!Files.isReadable(queryFilePath)) {
            System.err.println("QUERIES_PATH '" + queryFilePath.toAbsolutePath() + "' does not exist or is not readable");
            System.exit(1);
        }

        SortedMap<Integer, String> queries = new TreeMap<Integer, String>();
        try {
            File queryFile = new File(queryPath);
            Document queryDoc = Jsoup.parse(queryFile, "UTF-8", "");
            for(Element element : queryDoc.getElementsByTag("top")) {
                String queryNum = "";
                String title;
                String desc;
                Matcher matcher;

                // Get the query number
                Pattern queryNumPattern = Pattern.compile("Number: (\\d*)");
                matcher = queryNumPattern.matcher(element.text());
                matcher.find();
                queryNum = matcher.group(1);

                // Get the title
                title = element.selectFirst("title").text().replaceAll("[^a-zA-Z0-9\\s]", " ").strip();

                // Get the description
                String descStart = "Description:";
                String descEnd = "Narrative:";
                Pattern descPattern = Pattern.compile(
                        Pattern.quote(descStart) + "(.*?)" + Pattern.quote(descEnd)
                );
                matcher = descPattern.matcher(element.text());
                matcher.find();
                desc = matcher.group(1).replaceAll("[^a-zA-Z0-9\\s]", " ").strip();

                // append values to the dict
                queries.put(Integer.valueOf(queryNum), title + ' ' + desc);
            }

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }

        return queries;
    }
}
