package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alternative approach - reads feature name from the actual feature file
 */
public class AutoExtentReportPluginV2 implements EventListener {
    
    private static ExtentReports extent;
    private static Map<String, ExtentTest> featureMap = new ConcurrentHashMap<>();
    private static ThreadLocal<ExtentTest> scenarioTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> stepTest = new ThreadLocal<>();
    private static Map<String, String> featureNameCache = new ConcurrentHashMap<>();
    
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "Reports";
    private static String reportPath;
    
    static {
        initializeReport();
    }
    
    private static void initializeReport() {
        try {
            String reportFolder = BASE_DIR + File.separator + "Test-Reports-" + TIMESTAMP + File.separator;
            reportPath = reportFolder + "ExtentReport.html";
            
            Files.createDirectories(Paths.get(reportFolder));
            Files.createDirectories(Paths.get(reportFolder + "screenshots"));
            
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Automation Test Report");
            spark.config().setReportName("Cucumber Test Report");
            spark.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");
            
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Environment", "QA");
            
            DirectExtentManager.setExtentReports(extent);
            
            System.out.println("==========================================");
            System.out.println("AutoExtentReportPlugin V2 Initialized");
            System.out.println("Report: " + reportPath);
            System.out.println("==========================================");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }
    
    private void handleTestRunStarted(TestRunStarted event) {
        System.out.println("Test Run Started at: " + new Date());
    }
    
    private void handleTestCaseStarted(TestCaseStarted event) {
        try {
            String scenarioName = event.getTestCase().getName();
            String featureName = extractFeatureName(event.getTestCase());
            
            System.out.println("Feature: " + featureName + ", Scenario: " + scenarioName);
            
            ExtentTest feature = featureMap.computeIfAbsent(featureName, 
                name -> extent.createTest("Feature: " + name));
            
            ExtentTest scenario = feature.createNode("Scenario: " + scenarioName);
            scenarioTest.set(scenario);
            
            DirectExtentManager.setCurrentTest(scenario);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Enhanced feature name extraction - tries multiple methods
     */
    private String extractFeatureName(io.cucumber.plugin.event.TestCase testCase) {
        String uriString = testCase.getUri().toString();
        
        // Check cache first
        if (featureNameCache.containsKey(uriString)) {
            return featureNameCache.get(uriString);
        }
        
        String featureName = null;
        
        // Method 1: Try to read from the feature file directly
        featureName = readFeatureNameFromFile(uriString);
        
        // Method 2: Parse from URI path
        if (featureName == null || featureName.isEmpty()) {
            featureName = parseFeatureNameFromPath(uriString);
        }
        
        // Method 3: Use scenario name prefix if it looks like a feature
        if (featureName == null || featureName.isEmpty()) {
            String scenarioName = testCase.getName();
            if (scenarioName.contains(" - ")) {
                featureName = scenarioName.substring(0, scenarioName.indexOf(" - "));
            } else {
                featureName = "Test Suite";
            }
        }
        
        // Cache the result
        featureNameCache.put(uriString, featureName);
        
        return featureName;
    }
    
    /**
     * Read Feature: line from the actual feature file
     */
    private String readFeatureNameFromFile(String uri) {
        try {
            // Try to load as a classpath resource
            if (uri.contains("classpath:")) {
                String resourcePath = uri.substring(uri.indexOf("classpath:") + 10);
                InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
                
                if (is == null) {
                    // Try without leading slash
                    if (resourcePath.startsWith("/")) {
                        resourcePath = resourcePath.substring(1);
                    }
                    is = getClass().getClassLoader().getResourceAsStream(resourcePath);
                }
                
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("Feature:")) {
                                return line.substring(8).trim();
                            }
                        }
                    }
                }
            }
            
            // Try file system path
            if (uri.contains("file:")) {
                java.net.URI fileUri = new java.net.URI(uri);
                File file = new File(fileUri.getPath());
                
                if (file.exists()) {
                    for (String line : Files.readAllLines(file.toPath())) {
                        line = line.trim();
                        if (line.startsWith("Feature:")) {
                            return line.substring(8).trim();
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Could not read feature file: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Parse feature name from file path
     */
    private String parseFeatureNameFromPath(String uri) {
        try {
            String path = uri;
            
            // Remove protocol prefixes
            if (path.contains("classpath:")) {
                path = path.substring(path.indexOf("classpath:") + 10);
            } else if (path.contains("file:")) {
                path = path.substring(path.indexOf("file:") + 5);
                // Remove extra slashes
                while (path.startsWith("///")) {
                    path = path.substring(1);
                }
            }
            
            // Get filename
            if (path.contains("/")) {
                path = path.substring(path.lastIndexOf('/') + 1);
            }
            if (path.contains("\\")) {
                path = path.substring(path.lastIndexOf('\\') + 1);
            }
            
            // Remove extension
            if (path.endsWith(".feature")) {
                path = path.substring(0, path.length() - 8);
            }
            
            // Convert to readable format
            path = path.replace("_", " ").replace("-", " ");
            
            // Title case
            String[] words = path.split("\\s+");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    if (result.length() > 0) result.append(" ");
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "Test Suite";
        }
    }
    
    private void handleTestStepStarted(TestStepStarted event) {
        try {
            if (event.getTestStep() instanceof PickleStepTestStep) {
                PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
                String stepText = step.getStep().getKeyword() + step.getStep().getText();
                
                ExtentTest scenario = scenarioTest.get();
                if (scenario != null) {
                    ExtentTest stepNode = scenario.createNode(stepText);
                    stepTest.set(stepNode);
                    DirectExtentManager.setCurrentStep(stepNode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleTestStepFinished(TestStepFinished event) {
        try {
            if (event.getTestStep() instanceof PickleStepTestStep) {
                ExtentTest step = stepTest.get();
                if (step != null) {
                    Result result = event.getResult();
                    Status status = mapStatus(result.getStatus());
                    
                    if (result.getError() != null) {
                        step.log(status, "Step failed: " + result.getError().getMessage());
                    } else {
                        step.log(status, "Step " + status.toString().toLowerCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleTestCaseFinished(TestCaseFinished event) {
        try {
            ExtentTest scenario = scenarioTest.get();
            if (scenario != null) {
                Result result = event.getResult();
                Status status = mapStatus(result.getStatus());
                
                if (result.getError() != null) {
                    scenario.log(status, "Scenario failed: " + result.getError().getMessage());
                }
            }
        } finally {
            scenarioTest.remove();
            stepTest.remove();
            DirectExtentManager.setCurrentTest(null);
            DirectExtentManager.setCurrentStep(null);
        }
    }
    
    private void handleTestRunFinished(TestRunFinished event) {
        extent.flush();
        System.out.println("==========================================");
        System.out.println("Report Generated: " + reportPath);
        System.out.println("==========================================");
    }
    
    private Status mapStatus(io.cucumber.plugin.event.Status cucumberStatus) {
        if (cucumberStatus == null) return Status.INFO;
        
        switch (cucumberStatus) {
            case PASSED: return Status.PASS;
            case FAILED: return Status.FAIL;
            case SKIPPED: return Status.SKIP;
            case PENDING: return Status.WARNING;
            case AMBIGUOUS:
            case UNDEFINED: return Status.WARNING;
            default: return Status.INFO;
        }
    }
}
