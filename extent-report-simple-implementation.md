# Final Clean Solution - Single Report Initialization

## ❌ THE PROBLEM
- Report initialization was called in multiple places
- `@BeforeAll` doesn't work properly in Cucumber hooks
- Multiple initialization attempts cause confusion

## ✅ THE FIX
- Initialize ONLY ONCE in the Runner's static block
- Remove all other initialization calls
- Use a flag to track first scenario for logging

---

## 📄 FILE 1: `CucumberRunnerTests.java` (ONLY place for initialization)
```java
package com.example.runner;

import com.example.reporting.ReportConfiguration;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
    value = "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
    value = "com.example.stepdefs,com.example.hooks")
public class CucumberRunnerTests {
    
    // THIS IS THE ONLY PLACE WHERE INITIALIZATION HAPPENS
    static {
        System.out.println("=========================================");
        System.out.println("Initializing ExtentReports Configuration");
        System.out.println("=========================================");
        
        // Initialize report path and create dynamic extent.properties
        String reportPath = ReportConfiguration.initializeReportPath();
        
        System.out.println("Report will be generated at:");
        System.out.println(reportPath + "ExtentReport.html");
        System.out.println("=========================================");
        
        // Small delay to ensure file is written
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
```

---

## 📄 FILE 2: `ReportConfiguration.java` (No changes to logic, just cleanup)
```java
package com.example.reporting;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Creates dynamic extent.properties file with timestamped report path
 * Called ONLY from CucumberRunnerTests static block
 */
public class ReportConfiguration {
    
    private static String reportFolderPath;
    private static final String BASE_REPORT_DIR = System.getProperty("user.dir") + "/Reports/";
    private static boolean initialized = false;
    
    /**
     * Initialize report path and create dynamic extent.properties
     * This is called ONLY ONCE from CucumberRunnerTests static block
     */
    public static synchronized String initializeReportPath() {
        if (initialized) {
            return reportFolderPath;
        }
        
        try {
            // Create timestamped folder
            String timestamp = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
            reportFolderPath = BASE_REPORT_DIR + "Test-Reports-" + timestamp + "/";
            
            // Create directories
            Path reportPath = Paths.get(reportFolderPath);
            Files.createDirectories(reportPath);
            Files.createDirectories(Paths.get(reportFolderPath + "screenshots/"));
            
            // Create dynamic extent.properties in target/test-classes
            createDynamicExtentProperties(reportFolderPath);
            
            initialized = true;
            return reportFolderPath;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize report: " + e.getMessage());
            e.printStackTrace();
            return BASE_REPORT_DIR;
        }
    }
    
    /**
     * Create extent.properties file dynamically
     */
    private static void createDynamicExtentProperties(String reportPath) throws IOException {
        Properties props = new Properties();
        
        // Only Spark reporter (HTML)
        props.setProperty("extent.reporter.spark.start", "true");
        props.setProperty("extent.reporter.spark.out", reportPath + "ExtentReport.html");
        
        // Disable other reporters
        props.setProperty("extent.reporter.html.start", "false");
        props.setProperty("extent.reporter.json.start", "false");
        props.setProperty("extent.reporter.pdf.start", "false");
        
        // Spark configuration
        props.setProperty("extent.reporter.spark.theme", "STANDARD");
        props.setProperty("extent.reporter.spark.documentTitle", "Test Report");
        props.setProperty("extent.reporter.spark.reportName", "Android Automation Report");
        props.setProperty("extent.reporter.spark.timestampFormat", "dd-MM-yyyy HH:mm:ss");
        props.setProperty("extent.reporter.spark.base64imagesrc", "true");
        
        // System info
        props.setProperty("systeminfo.Platform", "Android");
        props.setProperty("systeminfo.Connection", "WiFi");
        props.setProperty("systeminfo.Environment", "QA");
        
        // Write to target/test-classes
        String targetPath = "target/test-classes/extent.properties";
        File targetDir = new File("target/test-classes");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        try (FileOutputStream out = new FileOutputStream(targetPath)) {
            props.store(out, "ExtentReports Configuration - Generated at runtime");
        }
        
        System.out.println("Created extent.properties at: " + targetPath);
    }
    
    /**
     * Get report folder path
     */
    public static String getReportFolderPath() {
        return reportFolderPath;
    }
    
    /**
     * Get screenshots folder path
     */
    public static String getScreenshotsFolderPath() {
        return getReportFolderPath() + "screenshots/";
    }
}
```

---

## 📄 FILE 3: `CucumberHooks.java` (NO @BeforeAll, use first scenario flag)
```java
package com.example.hooks;

import com.example.reporting.CustomExtentReporter;
import com.example.reporting.ReportConfiguration;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.*;
import java.net.URL;
import java.time.Duration;

/**
 * Cucumber Hooks for Android Appium testing
 * NO @BeforeAll as it doesn't work properly with Cucumber
 */
public class CucumberHooks {
    
    private static AndroidDriver driver;
    private static boolean firstScenario = true;
    private static long testSuiteStartTime;
    private static int scenarioCount = 0;
    private static int passedCount = 0;
    private static int failedCount = 0;
    
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            // Log test suite start info only for first scenario
            if (firstScenario) {
                firstScenario = false;
                testSuiteStartTime = System.currentTimeMillis();
                
                String reportPath = ReportConfiguration.getReportFolderPath();
                CustomExtentReporter.logInfo("==========================================");
                CustomExtentReporter.logInfo("Test Suite Execution Started");
                CustomExtentReporter.logInfo("Report Location: " + reportPath + "ExtentReport.html");
                CustomExtentReporter.logInfo("==========================================");
            }
            
            scenarioCount++;
            
            // Initialize Android driver
            driver = initializeAndroidDriver();
            
            CustomExtentReporter.logInfo("Starting Scenario #" + scenarioCount + ": " + scenario.getName());
            
            // Add device information to report
            CustomExtentReporter.addDeviceInfo(driver);
            
        } catch (Exception e) {
            CustomExtentReporter.logError("Failed to initialize test: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        try {
            // Update counts
            if (scenario.isFailed()) {
                failedCount++;
                CustomExtentReporter.logFail("Scenario Failed: " + scenario.getName());
                CustomExtentReporter.captureFailureScreenshot(driver, scenario.getName().replaceAll(" ", "_"));
            } else {
                passedCount++;
                CustomExtentReporter.logPass("Scenario Passed: " + scenario.getName());
            }
            
        } catch (Exception e) {
            CustomExtentReporter.logError("Error in after scenario: " + e.getMessage());
        } finally {
            // Clean up driver
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // Silent fail
                }
                driver = null;
            }
        }
    }
    
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Only capture screenshot on failure
        if (scenario.isFailed()) {
            CustomExtentReporter.captureAndAttachScreenshot(
                driver,
                "StepFailure_" + System.currentTimeMillis(),
                "Failed Step Screenshot"
            );
        }
    }
    
    /**
     * Initialize Android driver with WiFi connection
     */
    private AndroidDriver initializeAndroidDriver() {
        try {
            UiAutomator2Options options = new UiAutomator2Options();
            
            // Basic capabilities
            options.setPlatformName("Android");
            options.setAutomationName("UiAutomator2");
            options.setDeviceName(System.getProperty("device.name", "Android Device"));
            
            // App capabilities - UPDATE THESE
            options.setAppPackage(System.getProperty("app.package", "com.example.app"));
            options.setAppActivity(System.getProperty("app.activity", "com.example.app.MainActivity"));
            
            // WiFi optimization - Always true
            options.setNewCommandTimeout(Duration.ofSeconds(300));
            options.setCapability("noReset", true);
            options.setCapability("fullReset", false);
            options.setCapability("ignoreUnimportantViews", true);
            options.setCapability("disableWindowAnimation", true);
            options.setCapability("settings[waitForIdleTimeout]", 100);
            
            // Appium server URL
            String appiumServerUrl = System.getProperty("appium.server", "http://localhost:4723");
            
            AndroidDriver androidDriver = new AndroidDriver(new URL(appiumServerUrl), options);
            androidDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            return androidDriver;
            
        } catch (Exception e) {
            throw new RuntimeException("Driver initialization failed", e);
        }
    }
    
    /**
     * Get current driver instance
     */
    public static AndroidDriver getDriver() {
        return driver;
    }
    
    /**
     * This hook runs after ALL scenarios complete
     * Use this instead of @AfterAll which doesn't work properly
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (testSuiteStartTime > 0) {
                long totalDuration = System.currentTimeMillis() - testSuiteStartTime;
                
                System.out.println("\n==========================================");
                System.out.println("TEST SUITE EXECUTION COMPLETED");
                System.out.println("Total Scenarios: " + scenarioCount);
                System.out.println("Passed: " + passedCount);
                System.out.println("Failed: " + failedCount);
                System.out.println("Duration: " + (totalDuration / 1000) + " seconds");
                System.out.println("Report: " + ReportConfiguration.getReportFolderPath() + "ExtentReport.html");
                System.out.println("==========================================\n");
            }
        }));
    }
}
```

---

## 📄 FILE 4: `CustomExtentReporter.java` (Simplified, no initialization)
```java
package com.example.reporting;

import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ExtentReporter for Android Appium testing with WiFi optimization
 * No initialization here - all handled by ReportConfiguration
 */
public class CustomExtentReporter {
    
    private static final float COMPRESSION_QUALITY = 0.8f;
    
    /**
     * Takes screenshot and returns Base64 string (optimized for WiFi)
     */
    public static String captureScreenshot(AndroidDriver driver, String screenshotName) {
        try {
            if (driver == null) {
                return null;
            }
            
            // Capture screenshot as Base64
            String base64Screenshot = driver.getScreenshotAs(OutputType.BASE64);
            
            // Compress the image
            String compressedBase64 = compressBase64Image(base64Screenshot);
            
            // Save to file for reference
            saveScreenshotToFile(compressedBase64, screenshotName);
            
            return compressedBase64;
            
        } catch (Exception e) {
            logError("Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Captures screenshot and adds to ExtentReport
     */
    public static void captureAndAttachScreenshot(AndroidDriver driver, String screenshotName, String description) {
        try {
            String base64Screenshot = captureScreenshot(driver, screenshotName);
            
            if (base64Screenshot != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .addScreenCaptureFromBase64String(base64Screenshot, description);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Captures screenshot on failure
     */
    public static void captureFailureScreenshot(AndroidDriver driver, String testName) {
        try {
            String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
            String screenshotName = testName + "_FAILURE_" + timestamp;
            
            String base64Screenshot = captureScreenshot(driver, screenshotName);
            
            if (base64Screenshot != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .fail("Test Failed - Screenshot attached")
                    .addScreenCaptureFromBase64String(base64Screenshot);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Save screenshot to file
     */
    private static void saveScreenshotToFile(String base64Image, String fileName) {
        try {
            String screenshotPath = ReportConfiguration.getScreenshotsFolderPath();
            if (screenshotPath != null) {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                FileUtils.writeByteArrayToFile(new File(screenshotPath + fileName + ".jpg"), imageBytes);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Compress Base64 image
     */
    private static String compressBase64Image(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(compressed);
            
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(COMPRESSION_QUALITY);
            }
            
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();
            
            return Base64.getEncoder().encodeToString(compressed.toByteArray());
            
        } catch (Exception e) {
            return base64Image; // Return original if compression fails
        }
    }
    
    // ============ LOGGING METHODS ============
    
    public static void logInfo(String message) {
        try {
            ExtentCucumberAdapter.getCurrentStep().info(message);
        } catch (Exception e) {
            System.out.println("[INFO] " + message);
        }
    }
    
    public static void logPass(String message) {
        try {
            ExtentCucumberAdapter.getCurrentStep()
                .pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
        } catch (Exception e) {
            System.out.println("[PASS] " + message);
        }
    }
    
    public static void logFail(String message) {
        try {
            ExtentCucumberAdapter.getCurrentStep()
                .fail(MarkupHelper.createLabel(message, ExtentColor.RED));
        } catch (Exception e) {
            System.out.println("[FAIL] " + message);
        }
    }
    
    public static void logWarning(String message) {
        try {
            ExtentCucumberAdapter.getCurrentStep()
                .warning(MarkupHelper.createLabel(message, ExtentColor.ORANGE));
        } catch (Exception e) {
            System.out.println("[WARN] " + message);
        }
    }
    
    public static void logError(String message) {
        try {
            ExtentCucumberAdapter.getCurrentStep()
                .fail(MarkupHelper.createLabel("ERROR: " + message, ExtentColor.RED));
        } catch (Exception e) {
            System.err.println("[ERROR] " + message);
        }
    }
    
    /**
     * Add data table to report
     */
    public static void addDataTable(String title, String[][] data) {
        try {
            ExtentCucumberAdapter.getCurrentStep().info(title);
            ExtentCucumberAdapter.getCurrentStep().info(MarkupHelper.createTable(data));
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Add device information
     */
    public static void addDeviceInfo(AndroidDriver driver) {
        try {
            if (driver != null && driver.getSessionId() != null) {
                Map<String, Object> sessionDetails = driver.getSessionDetails();
                
                String[][] deviceInfo = {
                    {"Property", "Value"},
                    {"Platform", String.valueOf(sessionDetails.getOrDefault("platformName", "Android"))},
                    {"Device", String.valueOf(sessionDetails.getOrDefault("deviceName", "Unknown"))},
                    {"App Package", String.valueOf(sessionDetails.getOrDefault("appPackage", "N/A"))},
                    {"Session ID", driver.getSessionId().toString()}
                };
                
                addDataTable("Device Information", deviceInfo);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
}
```

---

## 📄 FILE 5: `extent.properties` (Minimal)
```properties
# Minimal configuration - actual path set dynamically
extent.reporter.spark.start=true
extent.reporter.spark.base64imagesrc=true
```

---

## 📄 FILE 6: `pom.xml` (Key part only - surefire plugin)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <includes>
            <include>**/CucumberRunnerTests.java</include>
        </includes>
        
        <reuseForks>false</reuseForks>
        <forkCount>1</forkCount>
        
        <systemPropertyVariables>
            <!-- Appium Configuration -->
            <appium.server>http://localhost:4723</appium.server>
            <device.name>Android Device</device.name>
            
            <!-- App Configuration - UPDATE THESE -->
            <app.package>com.example.app</app.package>
            <app.activity>com.example.app.MainActivity</app.activity>
        </systemPropertyVariables>
        
        <argLine>-Xmx1024m</argLine>
    </configuration>
</plugin>
```

---

## 🎯 KEY POINTS OF THIS FIX:

### ✅ Single Initialization Point
- **ONLY** in `CucumberRunnerTests` static block
- Runs before ExtentCucumberAdapter loads
- No other initialization calls anywhere

### ✅ No @BeforeAll in Hooks
- Cucumber doesn't properly support @BeforeAll
- Use `firstScenario` flag to log once in @Before
- Use shutdown hook for final summary

### ✅ Clean Flow
```
1. CucumberRunnerTests static block → Initialize report path
2. First @Before → Log report location
3. Tests run → Screenshots saved to correct folder
4. Shutdown hook → Print final summary
```

### ✅ WiFi Settings Hardcoded
- All WiFi optimizations are hardcoded as `true`
- No need for application.properties settings
- Simpler configuration

## 🚀 TO RUN:
```bash
# Clean and run
mvn clean test

# Update your app details in pom.xml or pass as parameters:
mvn test -Dapp.package=your.app.package -Dapp.activity=your.MainActivity
```

## 📁 RESULT:
```
Reports/
└── Test-Reports-DDMMYYHHMMSS/
    ├── ExtentReport.html
    └── screenshots/
```

This solution ensures:
- Single initialization point
- No conflicting @BeforeAll
- Clean, simple flow
- Report in correct location
