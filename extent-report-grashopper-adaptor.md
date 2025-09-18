# Reliable ExtentReports Solution - Fixed

## 🔴 THE ROOT CAUSE
- ExtentCucumberAdapter initializes multiple times
- Each initialization creates a new folder
- The adapter caches the first configuration it reads
- Race condition between property file creation and adapter initialization

## ✅ THE SOLUTION
Use a **singleton pattern** with a **fixed timestamp** that persists across all initializations, and ensure ExtentCucumberAdapter always gets the same path.

---

## 📄 FILE 1: `ExtentManager.java` (NEW - Singleton Manager)
```java
package com.example.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Singleton manager for ExtentReports configuration
 * Ensures single initialization and consistent report path
 */
public class ExtentManager {
    
    private static ExtentManager instance;
    private static final Object lock = new Object();
    
    private String reportFolderPath;
    private String reportFilePath;
    private boolean initialized = false;
    
    // Use a fixed timestamp for the entire test run
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "Reports";
    
    private ExtentManager() {
        // Private constructor
    }
    
    /**
     * Get singleton instance
     */
    public static ExtentManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ExtentManager();
                    instance.initialize();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize report configuration ONCE
     */
    private void initialize() {
        if (initialized) {
            System.out.println("ExtentManager already initialized at: " + reportFolderPath);
            return;
        }
        
        try {
            // Create report folder with fixed timestamp
            reportFolderPath = BASE_DIR + File.separator + "Test-Reports-" + TIMESTAMP + File.separator;
            reportFilePath = reportFolderPath + "ExtentReport.html";
            
            // Create directories
            Path reportPath = Paths.get(reportFolderPath);
            Files.createDirectories(reportPath);
            
            // Create screenshots folder
            Path screenshotsPath = Paths.get(reportFolderPath + "screenshots");
            Files.createDirectories(screenshotsPath);
            
            // CRITICAL: Set system properties FIRST (before creating properties file)
            setSystemProperties();
            
            // Then create properties file
            createPropertiesFile();
            
            initialized = true;
            
            System.out.println("=========================================");
            System.out.println("ExtentManager Initialized Successfully");
            System.out.println("Report Folder: " + reportFolderPath);
            System.out.println("Report File: " + reportFilePath);
            System.out.println("=========================================");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize ExtentManager: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to current directory
            reportFolderPath = System.getProperty("user.dir") + File.separator;
            reportFilePath = reportFolderPath + "ExtentReport.html";
            setSystemProperties();
        }
    }
    
    /**
     * Set system properties for ExtentCucumberAdapter
     */
    private void setSystemProperties() {
        // CRITICAL: System properties take precedence over file properties
        System.setProperty("extent.reporter.spark.start", "true");
        System.setProperty("extent.reporter.spark.out", reportFilePath);
        System.setProperty("extent.reporter.spark.config", "");
        System.setProperty("extent.reporter.spark.theme", "STANDARD");
        System.setProperty("extent.reporter.spark.documentTitle", "Test Report");
        System.setProperty("extent.reporter.spark.reportName", "Automation Report");
        System.setProperty("extent.reporter.spark.base64imagesrc", "true");
        System.setProperty("extent.reporter.spark.timelineEnabled", "true");
        System.setProperty("screenshot.dir", reportFolderPath + "screenshots");
        
        // Disable other reporters
        System.setProperty("extent.reporter.html.start", "false");
        System.setProperty("extent.reporter.pdf.start", "false");
        System.setProperty("extent.reporter.json.start", "false");
        
        System.out.println("✓ System properties set for ExtentReports");
    }
    
    /**
     * Create extent.properties file
     */
    private void createPropertiesFile() {
        try {
            Properties props = new Properties();
            
            // Main settings
            props.setProperty("extent.reporter.spark.start", "true");
            props.setProperty("extent.reporter.spark.out", reportFilePath);
            props.setProperty("extent.reporter.spark.config", "");
            
            // Theme and appearance
            props.setProperty("extent.reporter.spark.theme", "STANDARD");
            props.setProperty("extent.reporter.spark.documentTitle", "Test Report");
            props.setProperty("extent.reporter.spark.reportName", "Automation Report");
            props.setProperty("extent.reporter.spark.base64imagesrc", "true");
            props.setProperty("extent.reporter.spark.timelineEnabled", "true");
            props.setProperty("extent.reporter.spark.thumbnails", "true");
            
            // Disable other reporters
            props.setProperty("extent.reporter.html.start", "false");
            props.setProperty("extent.reporter.pdf.start", "false");
            props.setProperty("extent.reporter.json.start", "false");
            props.setProperty("extent.reporter.klov.start", "false");
            
            // System info
            props.setProperty("systeminfo.Platform", "Android");
            props.setProperty("systeminfo.Environment", "QA");
            
            // Create in multiple locations to ensure it's found
            String[] locations = {
                "target" + File.separator + "test-classes" + File.separator + "extent.properties",
                "extent.properties",
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "extent-dynamic.properties"
            };
            
            for (String location : locations) {
                try {
                    File file = new File(location);
                    file.getParentFile().mkdirs();
                    
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        props.store(out, "ExtentReports Configuration - Generated at runtime");
                    }
                    
                    if (file.exists()) {
                        System.out.println("✓ Created properties at: " + file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    // Continue to next location
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create properties file: " + e.getMessage());
        }
    }
    
    /**
     * Get report folder path
     */
    public String getReportFolderPath() {
        return reportFolderPath;
    }
    
    /**
     * Get report file path
     */
    public String getReportFilePath() {
        return reportFilePath;
    }
    
    /**
     * Get screenshots folder path
     */
    public String getScreenshotsFolderPath() {
        return reportFolderPath + "screenshots" + File.separator;
    }
    
    /**
     * Force initialization (call from multiple places to ensure it happens)
     */
    public static void forceInitialization() {
        getInstance();
    }
}
```

---

## 📄 FILE 2: `CucumberRunnerTests.java` (Simplified)
```java
package com.example.runner;

import com.example.reporting.ExtentManager;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.BeforeSuite;

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
    
    // Initialize as early as possible
    static {
        System.out.println("CucumberRunnerTests: Initializing ExtentManager");
        ExtentManager.forceInitialization();
    }
    
    // Also try with @BeforeSuite (JUnit 5 Platform)
    @BeforeSuite
    public static void setup() {
        ExtentManager.forceInitialization();
    }
}
```

---

## 📄 FILE 3: `CucumberHooks.java` (Ensure initialization)
```java
package com.example.hooks;

import com.example.reporting.CustomExtentReporter;
import com.example.reporting.ExtentManager;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.*;
import java.io.File;
import java.net.URL;
import java.time.Duration;

public class CucumberHooks {
    
    private static AndroidDriver driver;
    private static boolean firstScenario = true;
    private static ExtentManager extentManager;
    
    // Static initialization block
    static {
        // Ensure ExtentManager is initialized
        extentManager = ExtentManager.getInstance();
    }
    
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            if (firstScenario) {
                firstScenario = false;
                
                // Double-check initialization
                ExtentManager.forceInitialization();
                
                String reportPath = extentManager.getReportFilePath();
                
                System.out.println("=========================================");
                System.out.println("First Scenario Starting");
                System.out.println("Report will be at: " + reportPath);
                System.out.println("=========================================");
                
                CustomExtentReporter.logInfo("Test Execution Started");
                CustomExtentReporter.logInfo("Report: " + reportPath);
            }
            
            // Initialize driver
            driver = initializeAndroidDriver();
            
            CustomExtentReporter.logInfo("Scenario: " + scenario.getName());
            
        } catch (Exception e) {
            System.err.println("Error in before scenario: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                CustomExtentReporter.logFail("FAILED: " + scenario.getName());
                if (driver != null) {
                    String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9]", "_");
                    CustomExtentReporter.captureFailureScreenshot(driver, safeName);
                }
            } else {
                CustomExtentReporter.logPass("PASSED: " + scenario.getName());
            }
        } catch (Exception e) {
            System.err.println("Error in after scenario: " + e.getMessage());
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // Silent
                }
                driver = null;
            }
        }
    }
    
    private AndroidDriver initializeAndroidDriver() {
        try {
            UiAutomator2Options options = new UiAutomator2Options();
            
            options.setPlatformName("Android");
            options.setAutomationName("UiAutomator2");
            options.setDeviceName("Android Device");
            
            // UPDATE WITH YOUR APP DETAILS
            options.setAppPackage("com.example.app");
            options.setAppActivity("com.example.MainActivity");
            
            // WiFi optimizations
            options.setNewCommandTimeout(Duration.ofSeconds(300));
            options.setCapability("noReset", true);
            options.setCapability("ignoreUnimportantViews", true);
            options.setCapability("disableWindowAnimation", true);
            
            String appiumUrl = System.getProperty("appium.server", "http://localhost:4723");
            return new AndroidDriver(new URL(appiumUrl), options);
            
        } catch (Exception e) {
            throw new RuntimeException("Driver init failed", e);
        }
    }
    
    public static AndroidDriver getDriver() {
        return driver;
    }
}
```

---

## 📄 FILE 4: `CustomExtentReporter.java` (Use ExtentManager)
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

public class CustomExtentReporter {
    
    private static final float COMPRESSION_QUALITY = 0.8f;
    private static final ExtentManager extentManager = ExtentManager.getInstance();
    
    /**
     * Capture screenshot
     */
    public static String captureScreenshot(AndroidDriver driver, String screenshotName) {
        try {
            if (driver == null) {
                return null;
            }
            
            String base64Screenshot = driver.getScreenshotAs(OutputType.BASE64);
            String compressedBase64 = compressBase64Image(base64Screenshot);
            
            // Save to file
            saveScreenshotToFile(compressedBase64, screenshotName);
            
            return compressedBase64;
            
        } catch (Exception e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Capture and attach screenshot
     */
    public static void captureAndAttachScreenshot(AndroidDriver driver, String screenshotName, String description) {
        try {
            String base64Screenshot = captureScreenshot(driver, screenshotName);
            
            if (base64Screenshot != null && ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .addScreenCaptureFromBase64String(base64Screenshot, description);
            }
        } catch (Exception e) {
            System.err.println("Attach screenshot failed: " + e.getMessage());
        }
    }
    
    /**
     * Capture failure screenshot
     */
    public static void captureFailureScreenshot(AndroidDriver driver, String testName) {
        try {
            String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
            String screenshotName = testName + "_FAIL_" + timestamp;
            
            String base64Screenshot = captureScreenshot(driver, screenshotName);
            
            if (base64Screenshot != null && ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .fail("Screenshot attached")
                    .addScreenCaptureFromBase64String(base64Screenshot);
            }
        } catch (Exception e) {
            System.err.println("Failure screenshot failed: " + e.getMessage());
        }
    }
    
    /**
     * Save screenshot to file
     */
    private static void saveScreenshotToFile(String base64Image, String fileName) {
        try {
            String screenshotPath = extentManager.getScreenshotsFolderPath();
            if (screenshotPath != null) {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                File file = new File(screenshotPath + fileName + ".jpg");
                FileUtils.writeByteArrayToFile(file, imageBytes);
            }
        } catch (Exception e) {
            // Silent
        }
    }
    
    /**
     * Compress image
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
            return base64Image;
        }
    }
    
    // Logging methods with null checks
    
    public static void logInfo(String message) {
        try {
            if (ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep().info(message);
            }
            System.out.println("[INFO] " + message);
        } catch (Exception e) {
            System.out.println("[INFO] " + message);
        }
    }
    
    public static void logPass(String message) {
        try {
            if (ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
            }
            System.out.println("[PASS] " + message);
        } catch (Exception e) {
            System.out.println("[PASS] " + message);
        }
    }
    
    public static void logFail(String message) {
        try {
            if (ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .fail(MarkupHelper.createLabel(message, ExtentColor.RED));
            }
            System.out.println("[FAIL] " + message);
        } catch (Exception e) {
            System.out.println("[FAIL] " + message);
        }
    }
    
    public static void logWarning(String message) {
        try {
            if (ExtentCucumberAdapter.getCurrentStep() != null) {
                ExtentCucumberAdapter.getCurrentStep()
                    .warning(MarkupHelper.createLabel(message, ExtentColor.ORANGE));
            }
            System.out.println("[WARN] " + message);
        } catch (Exception e) {
            System.out.println("[WARN] " + message);
        }
    }
}
```

---

## 📄 FILE 5: Alternative - Use Direct ExtentReports (If adapter still fails)
```java
package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;

/**
 * Direct ExtentReports usage if adapter continues to fail
 */
public class DirectExtentReporter {
    
    private static ExtentReports extent;
    private static ExtentTest test;
    private static ExtentSparkReporter sparkReporter;
    
    public static void initializeReport() {
        String reportPath = ExtentManager.getInstance().getReportFilePath();
        
        sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Test Report");
        sparkReporter.config().setReportName("Automation Report");
        
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Platform", "Android");
        extent.setSystemInfo("Environment", "QA");
    }
    
    public static void createTest(String testName) {
        test = extent.createTest(testName);
    }
    
    public static void logInfo(String message) {
        if (test != null) {
            test.info(message);
        }
    }
    
    public static void logPass(String message) {
        if (test != null) {
            test.pass(message);
        }
    }
    
    public static void logFail(String message) {
        if (test != null) {
            test.fail(message);
        }
    }
    
    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
```

---

## 🎯 KEY IMPROVEMENTS:

1. **Singleton Pattern** - ExtentManager ensures single initialization
2. **Fixed Timestamp** - Same timestamp for entire test run
3. **System Properties First** - Set before file creation
4. **File.separator** - Platform independent paths
5. **Multiple Property Locations** - Ensures adapter finds it
6. **Null Checks** - Prevents NPE in reporters

## 🚀 TO USE:

1. Add all files to your project
2. Run from IntelliJ: Right-click `CucumberRunnerTests` → Run
3. Check console for "ExtentManager Initialized Successfully"
4. Report will be in ONE folder: `Reports/Test-Reports-TIMESTAMP/ExtentReport.html`

## 🔧 IF STILL FAILING:

Consider switching from tech.grasshopper adapter to direct ExtentReports usage (see DirectExtentReporter) or try the official Cucumber adapter version.

```xml
<!-- Alternative: Official ExtentReports adapter -->
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports-cucumber7-adapter</artifactId>
    <version>1.14.0</version>
</dependency>
```

The singleton pattern with fixed timestamp should solve the multiple folder issue!
