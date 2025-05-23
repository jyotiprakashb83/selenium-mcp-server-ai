import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.testng.annotations.Test;
import org.testng.Assert;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SeleniumTestClient {
    private final String serverUrl = "http://localhost:8080/execute";
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Test
    public void testWebNavigation() throws Exception {
        String testSteps = "Navigate to en.wikipedia.org. Search for India. Take a screenshot";
        String response = sendTestSteps(testSteps);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response, Map.class);
        Assert.assertTrue((Boolean) result.get("passed"), "Test failed: " + result.get("details"));
    }

    private String sendTestSteps(String steps) throws Exception {
        String testXpaths = "\nsearch box //input[@Type='search']" +
                "\nsearch button xpath=//button[contains(@class, 'search')]";

        HttpPost post = new HttpPost(serverUrl);
        String requestBody = new ObjectMapper().writeValueAsString(
                Map.of(
                        "steps", steps,
                        "xpaths", testXpaths
                )
        );
        post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");

        try (var response = httpClient.execute(post)) {
            return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}