public List<Map<String, AttributeValue>> getDataFromTable(String hkPrimaryKey, String tableName) {
    List<Map<String, AttributeValue>> allItems = new ArrayList<>();
    Map<String, AttributeValue> lastEvaluatedKey = null;
    
    do {
        Map<String, AttributeValue> expressionAttributeValue = new HashMap<>();
        expressionAttributeValue.put(":partitionKey", AttributeValue.builder().s(hkPrimaryKey).build());
        
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("HK = :partitionKey")
            .expressionAttributeValues(expressionAttributeValue);
        
        // Add pagination token if exists
        if (lastEvaluatedKey != null) {
            requestBuilder.exclusiveStartKey(lastEvaluatedKey);
        }
        
        QueryResponse response = dynamoDbClient.query(requestBuilder.build());
        
        // Add items to result list
        allItems.addAll(response.items());
        
        // Get pagination token for next batch
        lastEvaluatedKey = response.lastEvaluatedKey();
        
        System.out.println("Fetched " + response.count() + " items (Total so far: " + allItems.size() + ")");
        
    } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());
    
    System.out.println("✓ Total items retrieved: " + allItems.size());
    return allItems;
}
