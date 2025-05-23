package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class McpServer {
    private final Map<String, WebDriver> drivers;
    private String currentSession;
    private final LlmClient llmClient;
    private final String browser;
    private final int port;
    private final Map<String, List<String>> supportedOperations;

    public McpServer(String configPath, String operationsPath) throws IOException {
        Properties config = loadConfig(configPath);
        this.browser = config.getProperty("browser", "chrome");
        this.port = Integer.parseInt(config.getProperty("server.port", "8080"));
        String llmEndpoint = config.getProperty("llm.endpoint", "http://localhost:11434");
        String llmModel = config.getProperty("llm.model", "gemma2:9b");
        String llmApiKey = config.getProperty("llm.apiKey", "");
        this.llmClient = new LlmClient(llmEndpoint, llmModel, llmApiKey);
        this.supportedOperations = loadOperations(operationsPath);
        this.drivers = new HashMap<>();
        this.currentSession = null;
    }

    private Properties loadConfig(String configPath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);
        }
        return props;
    }

    private Map<String, List<String>> loadOperations(String operationsPath) throws IOException {
        Properties ops = new Properties();
        try (FileInputStream fis = new FileInputStream(operationsPath)) {
            ops.load(fis);
        }
        return ops.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> (String) e.getKey(),
                        e -> Arrays.asList(((String) e.getValue()).split(","))));
    }

    private WebDriver initializeDriver(String browserType, String headless) {
        boolean isHeadless = Boolean.parseBoolean(headless);
        switch (browserType.toLowerCase()) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (isHeadless) {
                    firefoxOptions.addArguments("--headless");
                }
                return new FirefoxDriver(firefoxOptions);
            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                if (isHeadless) {
                    chromeOptions.addArguments("--headless=new");
                }
                return new ChromeDriver(chromeOptions);
        }
    }

    private By getLocator(String by, String value) {
        return switch (by.toLowerCase()) {
            case "id" -> By.id(value);
            case "css" -> By.cssSelector(value);
            case "xpath" -> By.xpath(value);
            case "name" -> By.name(value);
            case "tag" -> By.tagName(value);
            case "class" -> By.className(value);
            default -> throw new IllegalArgumentException("Unsupported locator strategy: " + by);
        };
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/execute", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> request = mapper.readValue(requestBody, Map.class);
                String testSteps = request.get("steps");

                String testXpaths = "\nsearch box xpath=//*[@data-qa='cm_inp_field_search']" +
                        "\nsearch button xpath=//*[@data-qa='cm_icon_search']";

                // Query LLM to convert steps to Selenium commands
                String llmPrompt = "You are a java testng test automation expert. Convert the following test steps into a JSON array of Selenium commands " +
                        " each command must have a 'type' from this list: " + supportedOperations.keySet() +
                        " and include all required parameters: " + supportedOperations + ". Steps: " + testSteps +
                        "\nUse following xpaths for values:"+ testXpaths +
                        "\nNote: Respond with only json array of commands not extra characters. Include all commands and keep in order of execution" +
                        "\nA Sample for response for your reference in order of execution:\n"+
                        "  {\"type\": \"navigate\", \"url\": \"http://qa.coach.com/?auto=true\"},\n" +
                        "  {\"type\": \"find_element\", \"by\": \"xpath\", \"value\": \"//input[@id=\\\"searchtextbox\\\"]\", \"timeout\": \"5000\"},\n" +
                        "  {\"type\": \"send_keys\", \"by\": \"xpath\", \"value\": \"//input[@id=\\\"searchtextbox\\\"]\", \"text\": \"shoes\", \"timeout\": \"5000\"},\n" +
                        "  {\"type\": \"click_element\", \"by\": \"xpath\", \"value\": \"//input[@id=\\\"textbox\\\"]\", \"timeout\": \"5000\"}";
                System.out.println("==========llmPrompt============");
                System.out.println(llmPrompt);
                System.out.println("================================");

                String llmResponse = llmClient.queryLlm(llmPrompt);
                llmResponse = llmResponse.replace("```","");

                System.out.println("==========llmResponse============");
                System.out.println(llmResponse);
                System.out.println("================================");
                List<Map<String, String>> commands = mapper.readValue(llmResponse, List.class);

                String sessionId = "chrome_" + System.currentTimeMillis();
                WebDriver driver = initializeDriver("chrome", "headless");
                drivers.put(sessionId, driver);
                currentSession = sessionId;
                System.out.println("currentSession: "+currentSession);


                // Execute commands
                StringBuilder result = new StringBuilder();
                boolean passed = true;
                for (Map<String, String> cmd : commands) {
                    Thread.sleep(5000);
                    try {
                        validateCommand(cmd);
                        String commandResult = executeCommand(cmd);
                        result.append("Command ").append(cmd.get("type")).append(": ").append(commandResult).append("\n");
                    } catch (Exception e) {
                        passed = false;
                        result.append("Command ").append(cmd.get("type")).append(" failed: ").append(e.getMessage()).append("\n");
                    }
                }

                // Send response
                String response = mapper.writeValueAsString(Map.of("passed", passed, "details", result.toString()));
                sendResponse(exchange, 200, response);
                driver.close();
                driver.quit();
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("MCP Server started on port " + port);
    }

    private void validateCommand(Map<String, String> cmd) {
        String type = cmd.get("type");
        String parameters = cmd.get("parameters");
        if (!supportedOperations.containsKey(type)) {
            throw new IllegalArgumentException("Unsupported command type: " + type);
        }
        List<String> requiredParams = supportedOperations.get(type);
        for (String param : requiredParams) {
            if (!cmd.containsKey(param) || cmd.get(param) == null || cmd.get(param).isEmpty()) {
                throw new IllegalArgumentException("Missing required parameter '" + param + "' for command: " + type);
            }
        }
    }

    private String executeCommand(Map<String, String> cmd) {
        String type = cmd.get("type");
        long timeoutMs = cmd.containsKey("timeout") ? Long.parseLong(cmd.get("timeout")) : 10000;
        System.out.println("Executing cmd: "+cmd);
        switch (type.toLowerCase()) {
//            case "start_browser":
//                String sessionId = cmd.get("browser") + "_" + System.currentTimeMillis();
//                WebDriver driver = initializeDriver(cmd.get("browser"), cmd.get("headless"));
//                drivers.put(sessionId, driver);
//                currentSession = sessionId;
//                return "Browser started with session_id: " + sessionId;
            case "navigate":
                WebDriver navDriver = getDriver();
                navDriver.get(cmd.get("url"));
                return "Navigated to " + cmd.get("url");
            case "find_element":
                WebDriver findDriver = getDriver();
                By findLocator = getLocator(cmd.get("by"), cmd.get("value"));
                new WebDriverWait(findDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.presenceOfElementLocated(findLocator));
                return "Element found";
            case "click_element":
                WebDriver clickDriver = getDriver();
                By clickLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement clickElement = new WebDriverWait(clickDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(clickLocator));
                clickElement.click();
                return "Element clicked";
            case "send_keys":
                WebDriver sendKeysDriver = getDriver();
                By sendKeysLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement sendKeysElement = new WebDriverWait(sendKeysDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(sendKeysLocator));
                sendKeysElement.clear();
                sendKeysElement.sendKeys(cmd.get("text"));
                return "Text '" + cmd.get("text") + "' entered into element";
            case "get_element_text":
                WebDriver getTextDriver = getDriver();
                By getTextLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement getTextElement = new WebDriverWait(getTextDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.presenceOfElementLocated(getTextLocator));
                String text = getTextElement.getText();
                return text;
            case "hover":
                WebDriver hoverDriver = getDriver();
                By hoverLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement hoverElement = new WebDriverWait(hoverDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(hoverLocator));
                new Actions(hoverDriver).moveToElement(hoverElement).perform();
                return "Hovered over element";
            case "drag_and_drop":
                WebDriver dndDriver = getDriver();
                By sourceLocator = getLocator(cmd.get("by"), cmd.get("value"));
                By targetLocator = getLocator(cmd.get("targetBy"), cmd.get("targetValue"));
                WebElement sourceElement = new WebDriverWait(dndDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(sourceLocator));
                WebElement targetElement = new WebDriverWait(dndDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(targetLocator));
                new Actions(dndDriver).dragAndDrop(sourceElement, targetElement).perform();
                return "Drag and drop completed";
            case "double_click":
                WebDriver dcDriver = getDriver();
                By dcLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement dcElement = new WebDriverWait(dcDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(dcLocator));
                new Actions(dcDriver).doubleClick(dcElement).perform();
                return "Double click performed";
            case "right_click":
                WebDriver rcDriver = getDriver();
                By rcLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement rcElement = new WebDriverWait(rcDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.elementToBeClickable(rcLocator));
                new Actions(rcDriver).contextClick(rcElement).perform();
                return "Right click performed";
            case "press_key":
                WebDriver pkDriver = getDriver();
                String key = cmd.get("key").toUpperCase();
                Keys seleniumKey;
                try {
                    seleniumKey = Keys.valueOf(key);
                } catch (IllegalArgumentException e) {
                    seleniumKey = Keys.valueOf(key.charAt(0) + key.substring(1).toLowerCase());
                }
                new Actions(pkDriver).sendKeys(seleniumKey).perform();
                return "Key '" + key + "' pressed";
            case "upload_file":
                WebDriver ufDriver = getDriver();
                By ufLocator = getLocator(cmd.get("by"), cmd.get("value"));
                WebElement ufElement = new WebDriverWait(ufDriver, Duration.ofMillis(timeoutMs))
                        .until(ExpectedConditions.presenceOfElementLocated(ufLocator));
                ufElement.sendKeys(cmd.get("filePath"));
                return "File upload initiated";
            case "take_screenshot":
                WebDriver ssDriver = getDriver();
                String screenshotBase64 = ((TakesScreenshot) ssDriver).getScreenshotAs(OutputType.BASE64);
                byte[] imageBytes = Base64.getDecoder().decode(screenshotBase64);

                // Specify the path where you want to save the screenshot
                String filePath = "src/screenshots/screenshot_"+System.currentTimeMillis()+".png";
                File outputFile = new File(filePath);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(imageBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "Screenshot captured as base64: " + screenshotBase64;
            case "close_session":
                WebDriver closeDriver = getDriver();
                closeDriver.quit();
                drivers.remove(currentSession);
                String closedSession = currentSession;
                currentSession = null;
                return "Browser session " + closedSession + " closed";
            default:
                throw new IllegalArgumentException("Unknown command type: " + type);
        }
    }

    private WebDriver getDriver() {
        WebDriver driver = drivers.get(currentSession);
        if (driver == null) {
            throw new IllegalStateException("No active browser session");
        }
        return driver;
    }

    private void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void stop() {
        for (WebDriver driver : drivers.values()) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
        drivers.clear();
        currentSession = null;
    }

    public static void main(String[] args) throws IOException {
        McpServer server = new McpServer("src/main/resources/config.properties", "src/main/resources/selenium_operations.properties");
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}