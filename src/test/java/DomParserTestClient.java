import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DomParserTestClient {
    private final String serverUrl = "http://localhost:8080/execute";
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Test
    public void testWebNavigation() throws Exception {
        String testSteps = "Navigate to www.automationexercise.com. Click on Madame text";
        String response = sendTestSteps(testSteps);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response, Map.class);
        Assert.assertTrue((Boolean) result.get("passed"), "Test failed: " + result.get("details"));
    }

    private String sendTestSteps(String steps) throws Exception {
        String domElements = "{\n" +
                "  \"interactiveElements\" : [ {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/\",\n" +
                "    \"text\" : \"Home\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/products\",\n" +
                "    \"text\" : \"\uE8F8 Products\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/view_cart\",\n" +
                "    \"text\" : \"Cart\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/login\",\n" +
                "    \"text\" : \"Signup / Login\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/test_cases\",\n" +
                "    \"text\" : \"Test Cases\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/api_list\",\n" +
                "    \"text\" : \"API Testing\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"https://www.youtube.com/c/AutomationExercise\",\n" +
                "    \"text\" : \"Video Tutorials\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/contact_us\",\n" +
                "    \"text\" : \"Contact us\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"test_cases_list\",\n" +
                "    \"href\" : \"/test_cases\",\n" +
                "    \"text\" : \"Test Cases\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"apis_list\",\n" +
                "    \"href\" : \"/api_list\",\n" +
                "    \"text\" : \"APIs list for practice\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"test_cases_list\",\n" +
                "    \"href\" : \"/test_cases\",\n" +
                "    \"text\" : \"Test Cases\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"apis_list\",\n" +
                "    \"href\" : \"/api_list\",\n" +
                "    \"text\" : \"APIs list for practice\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"test_cases_list\",\n" +
                "    \"href\" : \"/test_cases\",\n" +
                "    \"text\" : \"Test Cases\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"apis_list\",\n" +
                "    \"href\" : \"/api_list\",\n" +
                "    \"text\" : \"APIs list for practice\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"left control-carousel hidden-xs\",\n" +
                "    \"href\" : \"#slider-carousel\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"class\" : \"right control-carousel hidden-xs\",\n" +
                "    \"href\" : \"#slider-carousel\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"#Women\",\n" +
                "    \"text\" : \"Women\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/1\",\n" +
                "    \"text\" : \"Dress\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/2\",\n" +
                "    \"text\" : \"Tops\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/7\",\n" +
                "    \"text\" : \"Saree\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"#Men\",\n" +
                "    \"text\" : \"Men\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/3\",\n" +
                "    \"text\" : \"Tshirts\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/6\",\n" +
                "    \"text\" : \"Jeans\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"#Kids\",\n" +
                "    \"text\" : \"Kids\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/4\",\n" +
                "    \"text\" : \"Dress\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/category_products/5\",\n" +
                "    \"text\" : \"Tops & Shirts\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Polo\",\n" +
                "    \"text\" : \"(6)Polo\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/H&M\",\n" +
                "    \"text\" : \"(5)H&M\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Madame\",\n" +
                "    \"text\" : \"(5)Madame\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Mast & Harbour\",\n" +
                "    \"text\" : \"(3)Mast & Harbour\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Babyhug\",\n" +
                "    \"text\" : \"(4)Babyhug\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Allen Solly Junior\",\n" +
                "    \"text\" : \"(3)Allen Solly Junior\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Kookie Kids\",\n" +
                "    \"text\" : \"(3)Kookie Kids\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/brand_products/Biba\",\n" +
                "    \"text\" : \"(5)Biba\"\n" +
                "  }, {\n" +
                "    \"tag\" : \"a\",\n" +
                "    \"href\" : \"/view_cart\",\n" +
                "    \"text\" : \"View Cart\"\n" +
                "  }]\n" +
                "}";

        HttpPost post = new HttpPost(serverUrl);
        String requestBody = new ObjectMapper().writeValueAsString(
                Map.of(
                        "steps", steps,
                        "dom", domElements
                )
        );
        post.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
        post.setHeader("Content-Type", "application/json");

        try (var response = httpClient.execute(post)) {
            return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}