# AWS Helper Library

A comprehensive Java Maven library designed to simplify AWS service interactions for test automation frameworks. This library provides easy-to-use helper classes for common AWS operations, enabling test teams to quickly integrate AWS validations and operations into their existing automation suites.

## 🚀 Features

### Database Services
- **DynamoDB**: Support for both standard operations and PartiQL transactions
- **RDS PostgreSQL**: Query execution and database operations
- **Amazon Athena**: Query execution with CSV export capabilities

### Storage & Data Processing
- **S3**: Complete file operations (upload, download, list, delete)
- **AWS Batch**: Job submission and monitoring
- **Step Functions**: Workflow execution and monitoring

### Compute & Serverless
- **AWS Lambda**: Function invocation and response handling
- **CloudWatch Logs**: Log retrieval and monitoring

### Authentication & Security
- **SRP Authentication**: Secure Remote Password protocol implementation
- **AWS Cognito**: Identity management integration
- **IAM**: Role-based access control

## 📦 Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.yourorganization</groupId>
    <artifactId>aws-helper-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 🛠️ Prerequisites

- Java 8 or higher
- Maven 3.6+
- AWS Account with appropriate permissions
- AWS CLI configured or environment variables set

## 📚 Core Classes Overview

### AWSBase
Foundation class providing AWS client builders and authentication methods.

**Key Features:**
- S3 credential management
- Multiple AWS client builders (DynamoDB, Athena, Step Functions, etc.)
- IAM role assumption capabilities
- JWT token management
- AWS Cognito integration

### Database Helpers

#### DynamoDBHelper
Standard DynamoDB operations for CRUD functionality.
```java
DynamoDBHelper dbHelper = new DynamoDBHelper();
// Perform standard DynamoDB operations
```

#### DynamoDBPartiQLTxnHelper
Advanced DynamoDB operations using PartiQL with transaction support.

**Functions:**
- `executeQuery()` - Execute PartiQL queries
- `ExecuteTransactionRequest()` - Handle transaction requests
- `getPartiQLTransactionStatements()` - Manage transaction statements
- `handleExecuteTransactionErrors()` - Error handling
- `handleCommonErrors()` - Common error management

#### RDSPostgresDBHelper
PostgreSQL database operations for RDS instances.
```java
RDSPostgresDBHelper rdsHelper = new RDSPostgresDBHelper();
// Execute queries on RDS PostgreSQL databases
```

#### AthenaQueryHelper
Amazon Athena query execution with CSV export capabilities.

**Functions:**
- `executeQueryAndExportToCSV()` - Execute query and export results
- `submitAthenaQuery()` - Submit queries for execution
- `waitForQueryToComplete()` - Monitor query completion
- `processResultRowsAndWriteToCsv()` - Process and export results
- `writeToCsv()` - CSV file generation

### Storage & File Operations

#### S3BucketHelper
Comprehensive S3 operations for file management.

**Functions:**
- `downloadLatestFileFromS3Bucket()` - Download most recent files
- `downloadS3BucketFiles()` - Bulk file downloads
- `uploadFileToBucket()` - File upload operations
- `downloadSpecificFileFromBucket()` - Targeted file downloads
- `getNumberOfObjectsInABucket()` - Bucket object counting
- `downloadAllFilesInAFolder()` - Folder-level downloads
- `isExistS3()` - Object existence verification

### Compute & Workflow

#### StepFunctionsHelper
AWS Step Functions workflow management.

**Functions:**
- `triggersStepFunction()` - Initiate workflow execution
- `getExecutionList()` - Retrieve execution history
- `getExeHistory()` - Detailed execution history

#### AWSLambdaHelper
AWS Lambda function invocation and management.
```java
AWSLambdaHelper lambdaHelper = new AWSLambdaHelper();
// Trigger Lambda functions and process responses
```

#### BatchJobHelper
AWS Batch job operations and monitoring.
```java
BatchJobHelper batchHelper = new BatchJobHelper();
// Submit and monitor batch jobs
```

### Monitoring & Logging

#### CloudWatchLogsHelper
CloudWatch logs retrieval and analysis.
```java
CloudWatchLogsHelper logsHelper = new CloudWatchLogsHelper();
// Retrieve and analyze CloudWatch logs
```

### Authentication

#### SRPAuthenticationHelper
Secure Remote Password authentication implementation.
```java
SRPAuthenticationHelper srpHelper = new SRPAuthenticationHelper();
// Perform secure authentication workflows
```

## 🔧 Configuration

### Environment Variables
Set up the following environment variables:
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_DEFAULT_REGION=your_preferred_region
```

### AWS Credentials
Ensure your AWS credentials are configured via:
- AWS CLI (`aws configure`)
- Environment variables
- IAM roles (for EC2 instances)
- AWS credentials file

## 💡 Usage Examples

### Basic S3 Operations
```java
S3BucketHelper s3Helper = new S3BucketHelper();

// Upload a file
s3Helper.uploadFileToBucket("my-bucket", "local-file.txt", "remote-key");

// Download latest file
String latestFile = s3Helper.downloadLatestFileFromS3Bucket("my-bucket", "prefix/");

// Check if object exists
boolean exists = s3Helper.isExistS3("my-bucket", "file-key");
```

### DynamoDB PartiQL Queries
```java
DynamoDBPartiQLTxnHelper dynamoHelper = new DynamoDBPartiQLTxnHelper();

// Execute a PartiQL query
String query = "SELECT * FROM MyTable WHERE id = ?";
dynamoHelper.executeQuery(query, parameters);
```

### Athena Query with CSV Export
```java
AthenaQueryHelper athenaHelper = new AthenaQueryHelper();

// Execute query and export to CSV
athenaHelper.executeQueryAndExportToCSV(
    "SELECT * FROM my_table LIMIT 100",
    "my-database",
    "s3://my-results-bucket/",
    "output.csv"
);
```

### Step Functions Execution
```java
StepFunctionsHelper stepHelper = new StepFunctionsHelper();

// Trigger a step function
stepHelper.triggersStepFunction("my-state-machine-arn", inputJson);

// Get execution history
stepHelper.getExeHistory("execution-arn");
```

## 🏗️ Architecture

The library follows a modular design pattern:
- **AWSBase**: Provides common AWS client configuration and authentication
- **Service-Specific Helpers**: Focused classes for each AWS service
- **Error Handling**: Comprehensive error management across all helpers
- **Configuration Management**: Flexible credential and region management

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Requirements

- **Java**: 8+
- **Maven**: 3.6+
- **AWS SDK**: Latest compatible version
- **Permissions**: Appropriate AWS IAM permissions for used services

## 🔐 Security Considerations

- Store AWS credentials securely
- Use IAM roles when possible
- Follow principle of least privilege
- Regularly rotate access keys
- Monitor AWS CloudTrail for audit logging

## 📞 Support

For issues, questions, or contributions, please:
1. Check existing documentation
2. Search through existing issues
3. Create a new issue with detailed information
4. Follow the contributing guidelines

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Note**: This library is designed for test automation purposes. Ensure proper error handling and security practices when integrating into production testing workflows.