import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class DomParser {
    // Configurable list of attributes to include for all interactive elements
    private static final List<String> INCLUDED_ATTRIBUTES = Arrays.asList(
            "id", "class", "type", "name", "value", "href", "aria-label", "role", "disabled"
    );

    // List of interactive element tags
    private static final List<String> INTERACTIVE_TAGS = Arrays.asList(
            "a", "button", "input", "select", "textarea", "form"
    );

    public static void main(String[] args) {
        // Example URL (can be replaced with user input)
        String url = "https://www.automationexercise.com/"; // Replace with args[0] for command-line input
        try {
            String jsonOutput = parseDomToJson(url);
            System.out.println(jsonOutput);
        } catch (IOException e) {
            System.err.println("Error processing URL: " + e.getMessage());
        }
    }

    public static String parseDomToJson(String url) throws IOException {
        // Make GET request and fetch HTML
        String htmlContent = fetchHtml(url);

        // Parse HTML with Jsoup
        Document doc = Jsoup.parse(htmlContent);

        // Create Jackson ObjectMapper for JSON generation
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode elementsArray = mapper.createArrayNode();

        // Iterate through interactive elements
        for (String tag : INTERACTIVE_TAGS) {
            Elements elements = doc.select(tag);
            for (Element element : elements) {
                ObjectNode elementNode = mapper.createObjectNode();
                elementNode.put("tag", element.tagName());

                // Add specified attributes
                for (String attr : INCLUDED_ATTRIBUTES) {
                    if (element.hasAttr(attr)) {
                        elementNode.put(attr, element.attr(attr));
                    }
                }

                // Add text content for relevant elements (e.g., buttons, links)
                if (tag.equals("a") || tag.equals("button")) {
                    String text = element.text().trim();
                    if (!text.isEmpty()) {
                        elementNode.put("text", text);
                    }
                }

                // Add form action for form elements
                if (tag.equals("form") && element.hasAttr("action")) {
                    elementNode.put("action", element.attr("action"));
                }

                elementsArray.add(elementNode);
            }
        }

        // Wrap in a root object
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("interactiveElements", elementsArray);

        // Convert to pretty-printed JSON
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    private static String fetchHtml(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP error code: " + responseCode);
        }

        StringBuilder content = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        connection.disconnect();
        return content.toString();
    }
}