# Zephyr Scale Test Automation Integration

A Java Maven project that enables seamless integration between automated test execution and Zephyr Scale for comprehensive test management and reporting.

## Overview

This project provides an automated solution for executing test cases in Zephyr Scale, with support for dynamic test cycle creation and real-time test result reporting. The integration extracts test case IDs from Cucumber scenario tags and automatically manages test execution lifecycle in Zephyr Scale.

## Features

- **Dynamic Test Cycle Management**: Automatically creates test cycles or uses existing ones
- **Cucumber Integration**: Extracts test case IDs from scenario tags (e.g., `@TestCaseID=TC-123`)
- **Real-time Result Reporting**: Updates test execution status in Zephyr Scale
- **Flexible Configuration**: Support for various Zephyr Scale environments and projects
- **Folder-based Organization**: Organize test cycles within specific folders
- **Error Handling**: Comprehensive exception handling and logging

## Prerequisites

- Java 8 or higher
- Maven 3.6+
- Zephyr Scale account with API access
- Valid Zephyr Scale API token
- Cucumber-based test framework

## Configuration

### Required Parameters

Set the following configuration parameters in your test environment:

```properties
# Zephyr Scale Configuration
zephyrAuthorizationKey=your_api_token_here
zephyrProjectKey=your_project_key
zephyrBaseURI=https://api.zephyrscale.smartbear.com/v2
testCycleFolderName=your_folder_name
enableZephyrExecution=true

# Optional Parameters
zephyrCycleID=existing_cycle_id (if using existing cycle)
env=environment_name (defaults to "defaultEnvironment")
```

### Environment Variables

You can also set these as environment variables:
- `ZEPHYR_AUTH_KEY`
- `ZEPHYR_PROJECT_KEY` 
- `ZEPHYR_BASE_URI`
- `TEST_CYCLE_FOLDER_NAME`
- `ENABLE_ZEPHYR_EXECUTION`

## Usage

### 1. Basic Setup

Initialize the ZephyrScale integration in your test setup:

```java
// The ZephyrScale class automatically reads configuration from properties
ZephyrScale zephyrScale = new ZephyrScale();
```

### 2. Cucumber Scenario Tagging

Tag your Cucumber scenarios with test case IDs:

```gherkin
@TestCaseID=TC-001
Scenario: User login functionality
  Given user is on login page
  When user enters valid credentials
  Then user should be logged in successfully

@TestCaseID=TC-002  
Scenario: Invalid login attempt
  Given user is on login page
  When user enters invalid credentials
  Then user should see error message
```

### 3. Test Execution

The integration automatically:
1. Checks if Zephyr execution is enabled
2. Creates or retrieves test cycle
3. Extracts test case IDs from scenario tags
4. Executes test cases in Zephyr Scale
5. Reports test results (PASS/FAIL)

### 4. Integration in Test Hooks

```java
@After
public void tearDown(Scenario scenario) {
    if (ZephyrParams.isCreatingTestCycle()) {
        String testCaseId = extractTestCaseId(scenario);
        String status = scenario.isFailed() ? "FAIL" : "PASS";
        zephyrScale.executeTestCase(testCaseId, status);
    }
}
```

## Configuration Options

### Test Cycle Management

**Option 1: Dynamic Cycle Creation**
- Set `testCycleFolderName` to specify folder
- Leave `zephyrCycleID` empty
- System creates new cycle with timestamp

**Option 2: Existing Cycle**
- Set `zephyrCycleID` to existing cycle ID
- System uses the specified cycle

**Option 3: Folder-based Auto-selection**
- Set `testCycleFolderName`
- System finds or creates cycle in specified folder

### Execution Control

```properties
# Enable/disable Zephyr integration
enableZephyrExecution=true

# Control test cycle creation
createNewCycle=true
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── automation/
│           └── library/
│               └── zephyrScale/
│                   ├── ZephyrScale.java          # Main integration class
│                   ├── ZephyrParams.java         # Configuration management
│                   └── TestExecutionPOJO.java    # Data models
└── test/
    └── java/
        └── [your test packages]
```

## Key Methods

### ZephyrScale Class

- `executeScenario(Scenario scenario, String scenarioStatus)` - Main execution method
- `getFolderID()` - Retrieves or creates folder for test cycles
- `createTestCycle()` - Creates new test cycle
- `executeTestCase(String testCaseID, String testCaseStatus)` - Executes individual test case

### ZephyrParams Class

- `isCreatingTestCycle()` - Checks if new cycle creation is enabled
- `getZephyrCycleID()` - Gets configured cycle ID
- `setZephyrCycleFolderID(String folderID)` - Sets folder ID for cycle

## Error Handling

The integration includes comprehensive error handling for:
- Invalid API credentials
- Network connectivity issues
- Missing test case IDs
- Folder/cycle creation failures
- Test execution errors

## Logging

All operations are logged with appropriate levels:
- **INFO**: Normal operation flow
- **ERROR**: Failures and exceptions
- **DEBUG**: Detailed execution information

## Best Practices

1. **Tag Consistency**: Ensure all scenarios have unique `@TestCaseID` tags
2. **Environment Separation**: Use different project keys for different environments
3. **Cycle Management**: Use descriptive folder names for better organization
4. **Error Monitoring**: Monitor logs for failed executions
5. **API Limits**: Be aware of Zephyr Scale API rate limits

## Troubleshooting

### Common Issues

**Issue**: Test cases not executing in Zephyr
**Solution**: Verify `enableZephyrExecution=true` and valid API credentials

**Issue**: Test cycle not found
**Solution**: Check folder name and ensure proper permissions

**Issue**: Test case ID extraction fails
**Solution**: Verify scenario tags format: `@TestCaseID=TC-XXX`

### Debug Mode

Enable detailed logging by setting log level to DEBUG:

```properties
logging.level.automation.library.zephyrScale=DEBUG
```

## API Documentation

For detailed Zephyr Scale API documentation, visit:
https://support.smartbear.com/zephyr-scale-cloud/api-docs/

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review Zephyr Scale API documentation
3. Verify configuration parameters
4. Check application logs for detailed error messages

## License

[Add your license information here]

## Contributing

[Add contribution guidelines here]

---

**Note**: Ensure you have proper permissions in Zephyr Scale for creating test cycles and executing test cases before using this integration.