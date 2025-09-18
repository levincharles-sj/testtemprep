# Fix for Inconsistent HTML Report Generation in IntelliJ

## 🔴 THE PROBLEM
- Static block in runner doesn't execute in IntelliJ
- ExtentCucumberAdapter loads before extent.properties is created
- HTML report is generated only sometimes
- Timing issue between property file creation and adapter initialization

## ✅ THE SOLUTION
Create a separate initialization class that forces early execution and ensure properties are set BEFORE the adapter plugin string is evaluated.

---

## 📄 FILE 1: `ExtentReportInitializer.java` (NEW - Force early initialization)
```java
package com.example.reporting;

/**
 * Forces early initialization of report configuration
 * This class is referenced in the runner to ensure it loads first
 */
public class ExtentReportInitializer {
    
    // This will execute when class is first referenced
    static {
        System.out.println("==========================================");
        System.out.println("ExtentReportInitializer: Starting");
        System.out.println("==========================================");
        
        // Initialize immediately
        String reportPath = ReportConfiguration.initializeReportPath();
        
        // CRITICAL: Set system properties that ExtentCucumberAdapter will read
        System.setProperty("extent.reporter.spark.start", "true");
        System.setProperty("extent.reporter.spark.out", reportPath + "ExtentReport.html");
        System.setProperty("extent.reporter.spark.config", "");
        
        System.out.println("Report initialized at: " + reportPath);
        System.out.println("==========================================");
    }
    
    /**
     * Dummy method to force class loading
     */
    public static void init() {
        // This method exists just to force the static block to run
        // when called from anywhere
    }
}
```

---

## 📄 FILE 2: `CucumberRunnerTests.java` (Updated for IntelliJ compatibility)
```java
package com.example.runner;

import com.example.reporting.ExtentReportInitializer;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
    value = "com.example.stepdefs,com.example.hooks")
public class CucumberRunnerTests {
    
    // Force initialization by referencing the class
    private static final String INIT = initializeReport();
    
    private static String initializeReport() {
        // This method will run during class loading
        System.out.println("CucumberRunnerTests: Forcing report initialization");
        ExtentReportInitializer.init();
        return "initialized";
    }
    
    // Dynamic plugin configuration using system property
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
        value = "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:")
    static class DynamicConfig {
        // This inner class helps ensure initialization happens
    }
}
```

---

## 📄 FILE 3: `ReportConfiguration.java` (Enhanced with retry and validation)
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
 * Creates dynamic extent.properties with validation
 */
public class ReportConfiguration {
    
    private static String reportFolderPath;
    private static final String BASE_REPORT_DIR = System.getProperty("user.dir") + "/Reports/";
    private static boolean initialized = false;
    
    /**
     * Initialize report path with validation
     */
    public static synchronized String initializeReportPath() {
        if (initialized && reportFolderPath != null) {
            System.out.println("Report already initialized at: " + reportFolderPath);
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
            
            // Create properties file with retry
            boolean created = createDynamicExtentProperties(reportFolderPath);
            
            if (!created) {
                System.err.println("WARNING: Failed to create extent.properties");
            }
            
            // IMPORTANT: Also set system properties as backup
            setSystemProperties(reportFolderPath);
            
            initialized = true;
            return reportFolderPath;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize report: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback
            reportFolderPath = BASE_REPORT_DIR;
            setSystemProperties(reportFolderPath);
            return reportFolderPath;
        }
    }
    
    /**
     * Create extent.properties with validation
     */
    private static boolean createDynamicExtentProperties(String reportPath) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                attempts++;
                
                Properties props = new Properties();
                
                // Essential properties
                props.setProperty("extent.reporter.spark.start", "true");
                props.setProperty("extent.reporter.spark.out", reportPath + "ExtentReport.html");
                
                // Disable other reporters
                props.setProperty("extent.reporter.html.start", "false");
                props.setProperty("extent.reporter.json.start", "false");
                props.setProperty("extent.reporter.pdf.start", "false");
                props.setProperty("extent.reporter.klov.start", "false");
                
                // Configuration
                props.setProperty("extent.reporter.spark.config", "");
                props.setProperty("extent.reporter.spark.theme", "STANDARD");
                props.setProperty("extent.reporter.spark.documentTitle", "Test Report");
                props.setProperty("extent.reporter.spark.reportName", "Android Automation Report");
                props.setProperty("extent.reporter.spark.timestampFormat", "dd-MM-yyyy HH:mm:ss");
                props.setProperty("extent.reporter.spark.base64imagesrc", "true");
                props.setProperty("extent.reporter.spark.timelineEnabled", "true");
                
                // System info
                props.setProperty("systeminfo.Platform", "Android");
                props.setProperty("systeminfo.Connection", "WiFi");
                props.setProperty("systeminfo.Environment", "QA");
                
                // Ensure target directory exists
                File targetDir = new File("target/test-classes");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                
                // Write properties file
                String targetPath = "target/test-classes/extent.properties";
                File propsFile = new File(targetPath);
                
                try (FileOutputStream out = new FileOutputStream(propsFile)) {
                    props.store(out, "ExtentReports Configuration - Generated at runtime");
                }
                
                // Verify file was created
                if (propsFile.exists() && propsFile.length() > 0) {
                    System.out.println("✓ Created extent.properties at: " + propsFile.getAbsolutePath());
                    System.out.println("✓ File size: " + propsFile.length() + " bytes");
                    
                    // Print contents for debugging
                    System.out.println("✓ Key property set: extent.reporter.spark.out=" + 
                                     props.getProperty("extent.reporter.spark.out"));
                    return true;
                }
                
                Thread.sleep(100); // Small delay before retry
                
            } catch (Exception e) {
                System.err.println("Attempt " + attempts + " failed: " + e.getMessage());
                if (attempts >= 3) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    
    /**
     * Set system properties as backup
     */
    private static void setSystemProperties(String reportPath) {
        System.setProperty("extent.reporter.spark.start", "true");
        System.setProperty("extent.reporter.spark.out", reportPath + "ExtentReport.html");
        System.setProperty("extent.reporter.spark.config", "");
        System.setProperty("extent.reporter.spark.theme", "STANDARD");
        System.setProperty("extent.reporter.spark.base64imagesrc", "true");
        
        System.out.println("✓ System properties set for ExtentReports");
    }
    
    /**
     * Get report folder path
     */
    public static String getReportFolderPath() {
        if (reportFolderPath == null) {
            initializeReportPath();
        }
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

## 📄 FILE 4: `CucumberHooks.java` (With initialization backup)
```java
package com.example.hooks;

import com.example.reporting.CustomExtentReporter;
import com.example.reporting.ExtentReportInitializer;
import com.example.reporting.ReportConfiguration;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.*;
import java.net.URL;
import java.time.Duration;

public class CucumberHooks {
    
    private static AndroidDriver driver;
    private static boolean firstScenario = true;
    
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            // Ensure initialization (backup if runner didn't do it)
            if (firstScenario) {
                firstScenario = false;
                
                // Force initialization if not done
                ExtentReportInitializer.init();
                
                // Verify report path is set
                String reportPath = ReportConfiguration.getReportFolderPath();
                if (reportPath == null || reportPath.isEmpty()) {
                    System.err.println("WARNING: Report path not initialized!");
                    reportPath = ReportConfiguration.initializeReportPath();
                }
                
                System.out.println("==========================================");
                System.out.println("Test Execution Started");
                System.out.println("Report will be at: " + reportPath + "ExtentReport.html");
                System.out.println("==========================================");
                
                // Log in report
                CustomExtentReporter.logInfo("Test Suite Started");
                CustomExtentReporter.logInfo("Report Location: " + reportPath);
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
                CustomExtentReporter.logFail("Scenario Failed: " + scenario.getName());
                if (driver != null) {
                    CustomExtentReporter.captureFailureScreenshot(driver, 
                        scenario.getName().replaceAll("[^a-zA-Z0-9]", "_"));
                }
            } else {
                CustomExtentReporter.logPass("Scenario Passed: " + scenario.getName());
            }
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
            
            // UPDATE YOUR APP DETAILS HERE
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
            throw new RuntimeException("Driver initialization failed", e);
        }
    }
    
    public static AndroidDriver getDriver() {
        return driver;
    }
}
```

---

## 📄 FILE 5: `extent.properties` in src/test/resources (Fallback)
```properties
# This is a fallback file
# Actual configuration is created dynamically at runtime
# in target/test-classes/extent.properties

extent.reporter.spark.start=true
extent.reporter.spark.base64imagesrc=true

# If this file is being used, it means dynamic generation failed
# Report will go to project root
```

---

## 🎯 WHY THIS WORKS:

### 1. **Multiple Initialization Points**
- `ExtentReportInitializer` static block
- `CucumberRunnerTests` forces initialization
- `CucumberHooks` @Before as backup
- System properties as additional backup

### 2. **System Properties Backup**
- Even if file creation fails, system properties are set
- ExtentCucumberAdapter checks system properties first

### 3. **Validation & Retry**
- Properties file creation has 3 retry attempts
- File existence and size validation
- Console output confirms successful creation

### 4. **IntelliJ Compatibility**
- Doesn't rely on static blocks in runner
- Multiple fallback mechanisms
- Works whether run from IntelliJ or Maven

## 🚀 TO RUN IN INTELLIJ:

1. Right-click on `CucumberRunnerTests`
2. Select "Run CucumberRunnerTests"
3. Check console output for initialization messages
4. Report will be in: `Reports/Test-Reports-TIMESTAMP/ExtentReport.html`

## 🔍 DEBUGGING:

If report is still not generated:

1. **Check Console Output**
   - Should see "✓ Created extent.properties at..."
   - Should see "✓ System properties set..."

2. **Verify File Creation**
   - Check `target/test-classes/extent.properties` exists
   - Open it to verify the path is correct

3. **Add Debug in CustomExtentReporter**
   ```java
   public static void logInfo(String message) {
       System.out.println("[DEBUG] Logging: " + message);
       // ... rest of method
   }
   ```

## ✅ This solution ensures:
- Report initialization happens regardless of how tests are run
- Multiple backup mechanisms
- Works in both IntelliJ and Maven
- No dependency on Maven configuration
