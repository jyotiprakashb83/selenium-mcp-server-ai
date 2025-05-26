package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.json.JSONObject;
import java.io.*;
import java.time.Duration;
import java.util.Properties;

public class DomSubtreeExtractor {
    public static void main(String[] args) {
        // Set path to ChromeDriver
        //System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");

        // Load properties file
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/elements.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading properties file: " + e.getMessage());
            return;
        }

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        JSONObject result = new JSONObject();

        try {
            // Navigate to the target website
            driver.get("https://en.wikipedia.org/wiki/Main_Page");

            // Iterate through properties to extract DOM subtrees
            for (String elementName : props.stringPropertyNames()) {
                String locator = props.getProperty(elementName);
                String[] locatorParts = locator.split(":", 2);
                if (locatorParts.length != 2) {
                    System.err.println("Invalid locator format for " + elementName + ": " + locator);
                    continue;
                }

                String locatorType = locatorParts[0].trim();
                String locatorValue = locatorParts[1].trim();
                By byLocator;

                // Determine locator type
                switch (locatorType.toLowerCase()) {
                    case "css":
                        byLocator = By.cssSelector(locatorValue);
                        break;
                    case "id":
                        byLocator = By.id(locatorValue);
                        break;
                    case "xpath":
                        byLocator = By.xpath(locatorValue);
                        break;
                    default:
                        System.err.println("Unsupported locator type for " + elementName + ": " + locatorType);
                        continue;
                }

                try {
                    // Wait for element to be present and extract outerHTML
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(byLocator));
                    String outerHtml = element.getAttribute("outerHTML");
                    result.put(elementName, new JSONObject()
                            .put("locator", locator)
                            .put("outerHTML", outerHtml));
                } catch (Exception e) {
                    System.err.println("Error extracting DOM for " + elementName + ": " + e.getMessage());
                    result.put(elementName, new JSONObject()
                            .put("locator", locator)
                            .put("error", e.getMessage()));
                }
            }

            // Save results to a JSON file
            try (FileWriter file = new FileWriter("dom_subtrees.json")) {
                file.write(result.toString(2));
                System.out.println("DOM subtrees saved to dom_subtrees.json");
            } catch (IOException e) {
                System.err.println("Error saving JSON file: " + e.getMessage());
            }

        } finally {
            // Clean up
            driver.quit();
        }
    }
}
