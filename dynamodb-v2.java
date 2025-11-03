import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public void addToCSV(Map<String, AttributeValue> item, String... filePath) throws IOException {
    List<String> HeaderList = new ArrayList<>();
    List<String> keys = new ArrayList<>(item.keySet());
    
    String file = filePath.length > 0 ? filePath[0] : Constants.reportFolder + "DBEntries-" + Constants.TIMESTAMP + ".csv";
    
    File theDir = new File(Constants.reportFolder);
    if (!theDir.exists()) {
        theDir.mkdirs();
    }
    
    File csvFile = new File(file);
    boolean newFile = !csvFile.exists() || csvFile.length() == 0;
    
    BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
    
    if (newFile) {
        // Write headers for new file
        for (String key : keys) {
            writer.append(key).append(",");
        }
        writer.newLine();
        HeaderList = new ArrayList<>(keys);
    } else {
        // Read existing headers
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String headerLine = reader.readLine();
        reader.close();
        
        if (headerLine != null) {
            String[] existingHeaders = headerLine.split(",");
            HeaderList = new ArrayList<>(Arrays.asList(existingHeaders));
            
            // Check if new columns need to be added
            List<String> existingHeaderList = new ArrayList<>(Arrays.asList(existingHeaders));
            List<String> keysTemp = new ArrayList<>(keys);
            Collections.sort(keysTemp);
            Collections.sort(existingHeaderList);
            
            if (!existingHeaderList.equals(keysTemp)) {
                for (String h : keys) {
                    if (!HeaderList.contains(h)) {
                        HeaderList.add(h);
                    }
                }
                replaceLineInLog(HeaderList, headerLine, file);
                writer.close();
                writer = new BufferedWriter(new FileWriter(file, true));
            }
        }
    }
    
    // Write data row
    for (String header : HeaderList) {
        String val = "";
        if (item.containsKey(header)) {
            val = attributeValueToString(item.get(header));
        }
        writer.append(val.replace(",", ":")).append(",");
    }
    
    writer.newLine();
    writer.flush();
    writer.close();
}

// Simple helper to convert any AttributeValue to String
private String attributeValueToString(AttributeValue av) {
    if (av.s() != null) {
        return av.s();  // String
    }
    if (av.n() != null) {
        return av.n();  // Number
    }
    if (av.bool() != null) {
        return av.bool().toString();  // Boolean
    }
    if (av.nul() != null && av.nul()) {
        return "";  // Null
    }
    if (av.hasM()) {
        return av.m().toString();  // Map - convert to string representation
    }
    if (av.hasL()) {
        return av.l().toString();  // List - convert to string representation
    }
    if (av.hasSs()) {
        return String.join(";", av.ss());  // String Set
    }
    if (av.hasNs()) {
        return String.join(";", av.ns());  // Number Set
    }
    return "";
}
