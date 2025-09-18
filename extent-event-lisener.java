package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.plugin.EventListener;  // Changed from ConcurrentEventListener
import io.cucumber.plugin.event.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Cucumber Plugin that automatically captures all steps
 * No manual step creation needed!
 */
public class AutoExtentReportPlugin implements EventListener {  // Changed to EventListener
    
    private static ExtentReports extent;
    private static Map<String, ExtentTest> featureMap = new ConcurrentHashMap<>();  // Thread-safe
    private static ThreadLocal<ExtentTest> scenarioTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> stepTest = new ThreadLocal<>();
    
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "Reports";
    private static String reportPath;
    
    static {
        initializeReport();
    }
    
    private static void initializeReport() {
        try {
            // Create report folder
            String reportFolder = BASE_DIR + File.separator + "Test-Reports-" + TIMESTAMP + File.separator;
            reportPath = reportFolder + "ExtentReport.html";
            
            Files.createDirectories(Paths.get(reportFolder));
            Files.createDirectories(Paths.get(reportFolder + "screenshots"));
            
            // Initialize ExtentReports
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Automation Test Report");
            spark.config().setReportName("Cucumber Test Report");
            spark.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");
            spark.config().setTimelineEnabled(false);  // Disable timeline for cleaner look
            
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Environment", "QA");
            extent.setSystemInfo("Framework", "Cucumber + Appium");
            extent.setSystemInfo("Execution Time", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
            
            // Make extent accessible to DirectExtentManager
            DirectExtentManager.setExtentReports(extent);
            
            System.out.println("==========================================");
            System.out.println("AutoExtentReportPlugin Initialized");
            System.out.println("Report: " + reportPath);
            System.out.println("==========================================");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize ExtentReports: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        // Test Run Started
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        
        // Test Case (Scenario) events
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        
        // Test Step events
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        
        // Test Run Finished
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }
    
    private void handleTestRunStarted(TestRunStarted event) {
        System.out.println("Test Run Started at: " + new Date());
    }
    
    private void handleTestCaseStarted(TestCaseStarted event) {
        try {
            String scenarioName = event.getTestCase().getName();
            String featureName = getFeatureName(event.getTestCase().getUri());
            
            // Get tags if any
            String tags = event.getTestCase().getTags().isEmpty() ? "" : 
                          String.join(", ", event.getTestCase().getTags());
            
            // Create or get feature test
            ExtentTest feature = featureMap.computeIfAbsent(featureName, 
                name -> {
                    ExtentTest featureTest = extent.createTest("Feature: " + name);
                    featureTest.assignCategory("Feature");
                    return featureTest;
                });
            
            // Create scenario under feature
            ExtentTest scenario = feature.createNode("Scenario: " + scenarioName);
            
            // Add tags as categories
            if (!tags.isEmpty()) {
                scenario.info("Tags: " + tags);
                for (String tag : event.getTestCase().getTags()) {
                    scenario.assignCategory(tag);
                }
            }
            
            scenarioTest.set(scenario);
            
            // Make available to DirectExtentManager
            DirectExtentManager.setCurrentTest(scenario);
            
        } catch (Exception e) {
            System.err.println("Error in handleTestCaseStarted: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleTestStepStarted(TestStepStarted event) {
        try {
            if (event.getTestStep() instanceof PickleStepTestStep) {
                PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
                String stepText = step.getStep().getKeyword() + step.getStep().getText();
                
                // Add argument if present
                if (step.getStep().getArgument() != null) {
                    if (step.getStep().getArgument() instanceof DocStringArgument) {
                        DocStringArgument docString = (DocStringArgument) step.getStep().getArgument();
                        stepText += "\n```\n" + docString.getContent() + "\n```";
                    } else if (step.getStep().getArgument() instanceof DataTableArgument) {
                        stepText += " [DataTable]";
                    }
                }
                
                ExtentTest scenario = scenarioTest.get();
                if (scenario != null) {
                    ExtentTest stepNode = scenario.createNode(stepText);
                    stepTest.set(stepNode);
                    
                    // Make available to DirectExtentManager
                    DirectExtentManager.setCurrentStep(stepNode);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in handleTestStepStarted: " + e.getMessage());
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
                        // Log the error details
                        String errorMessage = result.getError().getMessage();
                        if (errorMessage != null && errorMessage.length() > 500) {
                            errorMessage = errorMessage.substring(0, 500) + "...";
                        }
                        step.log(status, "Step failed: " + errorMessage);
                        
                        // Add stack trace in a collapsible section
                        String stackTrace = getStackTrace(result.getError());
                        if (stackTrace != null && !stackTrace.isEmpty()) {
                            step.fail("<details><summary>Stack Trace</summary><pre>" + 
                                     stackTrace + "</pre></details>");
                        }
                    } else {
                        String message = "Step " + status.toString().toLowerCase();
                        
                        // Add duration if available
                        if (result.getDuration() != null) {
                            long durationMs = result.getDuration().toMillis();
                            message += " (" + durationMs + " ms)";
                        }
                        
                        step.log(status, message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in handleTestStepFinished: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleTestCaseFinished(TestCaseFinished event) {
        try {
            ExtentTest scenario = scenarioTest.get();
            if (scenario != null) {
                Result result = event.getResult();
                Status status = mapStatus(result.getStatus());
                
                // Add scenario result summary
                String summary = "Scenario " + status.toString().toLowerCase();
                
                // Add duration
                if (result.getDuration() != null) {
                    long durationMs = result.getDuration().toMillis();
                    summary += " in " + durationMs + " ms";
                }
                
                if (result.getError() != null) {
                    scenario.log(status, summary + " - " + result.getError().getMessage());
                } else {
                    scenario.log(status, summary);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in handleTestCaseFinished: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up thread locals
            scenarioTest.remove();
            stepTest.remove();
            DirectExtentManager.setCurrentTest(null);
            DirectExtentManager.setCurrentStep(null);
        }
    }
    
    private void handleTestRunFinished(TestRunFinished event) {
        try {
            extent.flush();
            System.out.println("==========================================");
            System.out.println("Report Generated Successfully!");
            System.out.println("Location: " + reportPath);
            System.out.println("==========================================");
        } catch (Exception e) {
            System.err.println("Error flushing report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Status mapStatus(io.cucumber.plugin.event.Status cucumberStatus) {
        if (cucumberStatus == null) {
            return Status.INFO;
        }
        
        switch (cucumberStatus) {
            case PASSED:
                return Status.PASS;
            case FAILED:
                return Status.FAIL;
            case SKIPPED:
                return Status.SKIP;
            case PENDING:
                return Status.WARNING;
            case AMBIGUOUS:
            case UNDEFINED:
                return Status.WARNING;
            default:
                return Status.INFO;
        }
    }
    
    private String getFeatureName(java.net.URI uri) {
        if (uri == null) {
            return "Unknown Feature";
        }
        
        try {
            String path = uri.getPath();
            if (path == null) {
                return "Unknown Feature";
            }
            
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            return fileName.replace(".feature", "");
        } catch (Exception e) {
            return "Unknown Feature";
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            // Limit stack trace length
            if (stackTrace.length() > 2000) {
                stackTrace = stackTrace.substring(0, 2000) + "\n... (truncated)";
            }
            
            return stackTrace;
        } catch (Exception e) {
            return "Unable to get stack trace";
        }
    }
}
