**Selenium MCP Server**

This project implements a Selenium Master Control Program (MCP) server in Java that executes browser automation tasks by converting natural language test steps into Selenium WebDriver commands using a Language Model (LLM). The server supports local Ollama or remote LLM endpoints and is configurable via property files. A TestNG-based client is included to send test prompts and log results.
Project Structure

* src/main/java/**McpServer**.java: Main server class that handles HTTP requests, LLM interaction, and Selenium command execution.
* src/main/java/**LlmClient**.java: Handles communication with the LLM (Ollama or remote API).
* src/test/java/**SeleniumTestClient**.java: TestNG client that sends test steps to the server and logs results.
* **config.properties**: Configuration file for server settings (port, browser, LLM endpoint).
* **selenium_operations.properties**: Defines supported Selenium operations and their required parameters.
* **pom.xml**: Maven configuration with dependencies.

**Prerequisites**

* Java: 17 or later
* Maven: 3.6.0 or later
* WebDriver: ChromeDriver or FirefoxDriver (matching your browser version) installed and added to PATH
* LLM: Either:
* Local Ollama running on http://localhost:11434 (default) with a model like llama3
* A remote LLM endpoint (e.g., OpenAI-compatible API) with an API key
* Browser: Chrome or Firefox installed
* TestNG: Included via Maven, no separate installation needed

**Setup**

* Clone the Project:
* git clone <repository-url>
* cd selenium-mcp


Install Dependencies:Run the following command to download dependencies:
mvn install


**Configure WebDriver:**

Download ChromeDriver (for Chrome) or GeckoDriver (for Firefox) from their respective websites.
Ensure the driver is in your system PATH or specify its location in config.properties if needed.


Configure the Server:Edit config.properties in the project root to set:

* **server.port**: HTTP server port (default: 8080)
* **browser**: Browser type (chrome or firefox, default: chrome)
* **llm.endpoint**: LLM API URL (default: http://localhost:11434 for Ollama)
* **llm.model**: LLM model name (default: llama3)
* **llm.apiKey**: API key for remote LLM (leave empty for Ollama)


**Sample config.properties:**
* server.port=8080
* browser=chrome
* llm.endpoint=http://localhost:11434
* llm.model=llama3
* llm.apiKey=


Configure Selenium Operations:The selenium_operations.properties file defines supported Selenium commands and their parameters. It includes operations like start_browser, navigate, click_element, and more. Modify this file to add or change operations if needed.
Example selenium_operations.properties:
start_browser=browser,headless
navigate=url
click_element=by,value,timeout

# ... (other operations)

**Set Up Ollama (if using local LLM)**:

Install Ollama (see Ollama documentation).
Start Ollama server:

`ollama serve`

Ensure the specified model (e.g., llama3) is pulled:ollama pull llama3

**Running the Server**

Start the Server:Run the McpServer class:

`mvn exec:java -Dexec.mainClass="McpServer"`

Or, use your IDE to run McpServer.java.
The server will start on the configured port (default: http://localhost:8080) and listen for POST requests to /execute.

Verify Server:Check the console for:
MCP Server started on port 8080


**Running the Client**

Run Tests:Execute the TestNG client to send test steps and generate results:
mvn test

Or, run SeleniumTestClient.java in your IDE with the TestNG plugin.
The client sends a sample test prompt (e.g., navigating to https://example.com, clicking a link, and verifying text) and logs results using TestNG.

View Test Results:TestNG generates reports in the test-output/ directory (HTML and XML formats) with pass/fail status and details.
Example test steps in SeleniumTestClient.java:

String testSteps = `"Start a chrome browser, navigate to https://example.com, click the link with xpath //a[text()='More information'], get the text of xpath //body."`;

**Example LLM Output**
The LLM must convert test steps into a JSON array of commands matching the operations in selenium_operations.properties. Example:

[

{"type": "start_browser", "browser": "chrome", "headless": "false"},

{"type": "navigate", "url": "https://example.com"},

{"type": "click_element", "by": "xpath", "value": "//a[text()='More information']", "timeout": "10000"},

{"type": "get_element_text", "by": "xpath", "value": "//body", "timeout": "10000"},

{"type": "close_session"}

]

**Supported Selenium Operations**:
The server supports the following operations (defined in **selenium_operations.properties**):

* **start_browser**: Launches a browser session (browser, headless).
* **navigate**: Navigates to a URL (url).
* **find_element**: Locates an element (by, value, timeout).
* **click_element**: Clicks an element (by, value, timeout).
* **send_keys**: Sends text to an element (by, value, text, timeout).
* **get_element_text**: Retrieves element text (by, value, timeout).
* **hover**: Hovers over an element (by, value, timeout).
* **drag_and_drop**: Drags one element to another (by, value, targetBy, targetValue, timeout).
* **double_click**: Double-clicks an element (by, value, timeout).
* **right_click**: Right-clicks an element (by, value, timeout).
* **press_key**: Simulates a key press (key).
* **upload_file**: Uploads a file (by, value, filePath, timeout).
* **take_screenshot**: Captures a screenshot as base64 (outputPath optional).
* **close_session**: Closes the current browser session.

**Notes**

**LLM Configuration**: Ensure the LLM returns structured JSON commands matching the operations in selenium_operations.properties. Adjust the prompt in McpServer.java if needed.
WebDriver Compatibility: Ensure ChromeDriver or GeckoDriver matches your browser version.
File Uploads: The upload_file operation requires a valid file path accessible to the server.
Screenshots: The take_screenshot operation returns base64 data to avoid server-side file system dependencies.
Error Handling: The server catches and reports errors for each command in the response.
Extending Operations: Add new operations to selenium_operations.properties and update executeCommand in McpServer.java as needed.

**Troubleshooting**

Server Fails to Start: Check config.properties for valid settings and ensure the LLM endpoint is accessible.
WebDriver Errors: Verify ChromeDriver/GeckoDriver is in PATH and compatible with your browser.
LLM Issues: Ensure Ollama is running or the remote LLM endpoint and API key are correct.
Test Failures: Check TestNG reports in test-output/ for detailed error messages.

**License**
This project is licensed under the MIT License.
