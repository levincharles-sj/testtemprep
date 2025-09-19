I'll provide you with a comprehensive step-by-step guide to set up your Android automation testing environment on Windows.

## Complete Android Automation Setup Guide for Windows

### Part 1: Install Android Studio & SDK

1. **Download Android Studio**
   - Go to https://developer.android.com/studio
   - Download the Windows installer (exe file)
   - Run the installer and follow the setup wizard
   - Install to default location: `C:\Program Files\Android\Android Studio`

2. **Initial Android Studio Setup**
   - Launch Android Studio once after installation
   - Go through the initial setup wizard
   - Choose "Standard" installation type
   - Let it download the SDK components (this creates the SDK folder)
   - The SDK will be installed at: `C:\Users\%USERNAME%\AppData\Local\Android\Sdk`

### Part 2: Set Environment Variables

1. **Open System Environment Variables**
   ```cmd
   Windows Key + X → System → Advanced System Settings → Environment Variables
   ```

2. **Add ANDROID_HOME**
   - Click "New" under System Variables
   - Variable name: `ANDROID_HOME`
   - Variable value: `C:\Users\%USERNAME%\AppData\Local\Android\Sdk`

3. **Add JAVA_HOME** (if not already set)
   - Variable name: `JAVA_HOME`
   - Variable value: Path to your JDK installation (e.g., `C:\Program Files\Java\jdk-17`)

4. **Update PATH Variable**
   Add these paths to your PATH variable:
   ```
   %ANDROID_HOME%\platform-tools
   %ANDROID_HOME%\emulator
   %ANDROID_HOME%\tools
   %ANDROID_HOME%\tools\bin
   %ANDROID_HOME%\cmdline-tools\latest\bin
   %JAVA_HOME%\bin
   ```

5. **Verify Setup**
   Open new Command Prompt and test:
   ```cmd
   adb --version
   emulator -version
   java -version
   ```

### Part 3: Install SDK Components via Command Line

1. **Install SDK Manager Command Line Tools**
   ```cmd
   # Open Android Studio → SDK Manager → SDK Tools
   # Check "Android SDK Command-line Tools" and install
   ```

2. **Install Required SDK Components**
   ```cmd
   # List available packages
   sdkmanager --list

   # Install platform tools and build tools
   sdkmanager "platform-tools" "build-tools;34.0.0"
   
   # Install Android platform (choose version, e.g., API 34)
   sdkmanager "platforms;android-34"
   
   # Install system images for emulator (x86_64 for better performance)
   sdkmanager "system-images;android-34;google_apis_playstore;x86_64"
   
   # Install emulator
   sdkmanager "emulator"
   ```

### Part 4: Create Android Emulator

1. **Create AVD (Android Virtual Device)**
   ```cmd
   # List available device definitions
   avdmanager list device

   # Create AVD (example with Pixel 5)
   avdmanager create avd -n "Pixel_5_API_34" -k "system-images;android-34;google_apis_playstore;x86_64" -d "pixel_5"
   
   # Or with more options
   avdmanager create avd -n "TestDevice" -k "system-images;android-34;google_apis_playstore;x86_64" -d "pixel_5" -c 2048M -f
   ```

2. **List Created AVDs**
   ```cmd
   emulator -list-avds
   ```

### Part 5: Launch Emulator from Command Line

1. **Start Emulator**
   ```cmd
   # Basic launch
   emulator -avd Pixel_5_API_34
   
   # Launch with options (no audio, GPU acceleration)
   emulator -avd Pixel_5_API_34 -no-audio -gpu host
   
   # Launch in background (Windows)
   start /B emulator -avd Pixel_5_API_34
   ```

2. **Wait for Device to Boot**
   ```cmd
   # Check device status
   adb devices
   
   # Wait for device to be ready
   adb wait-for-device
   
   # Check if boot completed
   adb shell getprop sys.boot_completed
   ```

### Part 6: Install APK Files

1. **Install Single APK**
   ```cmd
   # Basic installation
   adb install path\to\your\app.apk
   
   # Reinstall (update existing app)
   adb install -r path\to\your\app.apk
   
   # Allow test packages
   adb install -t path\to\your\app.apk
   
   # Grant all permissions during install
   adb install -g path\to\your\app.apk
   ```

2. **Install Multiple APKs (Script)**
   Create `install_apks.bat`:
   ```batch
   @echo off
   echo Installing supporting APKs...
   
   :: List your supporting APKs here
   adb install -r -g "C:\apks\support_app1.apk"
   adb install -r -g "C:\apks\support_app2.apk"
   adb install -r -g "C:\apks\support_app3.apk"
   
   echo Installing main application...
   adb install -r -g "C:\apks\main_app.apk"
   
   echo All APKs installed successfully!
   pause
   ```

### Part 7: Install Node.js and Appium

1. **Install Node.js**
   - Download from https://nodejs.org/ (LTS version)
   - Run installer with default settings
   - Verify: `node --version` and `npm --version`

2. **Install Appium**
   ```cmd
   # Install Appium globally
   npm install -g appium
   
   # Install Appium Doctor (diagnostic tool)
   npm install -g appium-doctor
   
   # Install UiAutomator2 driver for Android
   appium driver install uiautomator2
   
   # Verify installation
   appium --version
   ```

3. **Run Appium Doctor**
   ```cmd
   appium-doctor --android
   ```

### Part 8: Setup Maven Project for Automation

1. **Install Maven** (if not installed)
   - Download from https://maven.apache.org/download.cgi
   - Extract to `C:\Program Files\Apache\maven`
   - Add `C:\Program Files\Apache\maven\bin` to PATH
   - Verify: `mvn --version`

2. **Create Maven Project Structure**
   ```
   my-android-automation/
   ├── pom.xml
   ├── src/
   │   ├── main/java/
   │   └── test/
   │       ├── java/
   │       │   └── com/yourcompany/tests/
   │       └── resources/
   │           └── testng.xml
   ```

3. **Maven pom.xml Configuration**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
            http://maven.apache.org/xsd/maven-4.0.0.xsd">
       <modelVersion>4.0.0</modelVersion>
       
       <groupId>com.yourcompany</groupId>
       <artifactId>android-automation</artifactId>
       <version>1.0-SNAPSHOT</version>
       
       <properties>
           <maven.compiler.source>11</maven.compiler.source>
           <maven.compiler.target>11</maven.compiler.target>
           <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
           <appium.version>9.1.0</appium.version>
           <testng.version>7.9.0</testng.version>
           <selenium.version>4.18.1</selenium.version>
       </properties>
       
       <dependencies>
           <dependency>
               <groupId>io.appium</groupId>
               <artifactId>java-client</artifactId>
               <version>${appium.version}</version>
           </dependency>
           
           <dependency>
               <groupId>org.seleniumhq.selenium</groupId>
               <artifactId>selenium-java</artifactId>
               <version>${selenium.version}</version>
           </dependency>
           
           <dependency>
               <groupId>org.testng</groupId>
               <artifactId>testng</artifactId>
               <version>${testng.version}</version>
           </dependency>
           
           <dependency>
               <groupId>org.slf4j</groupId>
               <artifactId>slf4j-simple</artifactId>
               <version>2.0.12</version>
           </dependency>
       </dependencies>
   </project>
   ```

### Part 9: Sample Test Class

Create `AndroidTest.java`:
```java
package com.yourcompany.tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.testng.annotations.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class AndroidTest {
    private AndroidDriver driver;
    
    @BeforeMethod
    public void setUp() throws MalformedURLException {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName("emulator-5554");  // Use adb devices to get this
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setApp("C:\\apks\\your-app.apk");  // Path to your APK
        // OR use if app is already installed:
        // options.setAppPackage("com.your.app.package");
        // options.setAppActivity("com.your.app.MainActivity");
        
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        options.setNoReset(false);
        
        driver = new AndroidDriver(
            new URL("http://127.0.0.1:4723"), 
            options
        );
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    @Test
    public void testExample() {
        // Your test code here
        // Example: driver.findElement(By.id("com.example:id/button")).click();
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

### Part 10: Complete Automation Workflow Script

Create `run_automation.bat`:
```batch
@echo off
echo Starting Android Automation Setup...

:: Start emulator in background
echo Starting emulator...
start /B emulator -avd Pixel_5_API_34 -no-audio -gpu host

:: Wait for emulator to boot
echo Waiting for device to boot...
adb wait-for-device

:: Wait for boot completion
:WAIT_BOOT
adb shell getprop sys.boot_completed | findstr "1" >nul
if %errorlevel% neq 0 (
    timeout /t 5 >nul
    goto WAIT_BOOT
)
echo Device ready!

:: Install APKs
echo Installing APKs...
call install_apks.bat

:: Start Appium server
echo Starting Appium server...
start cmd /k appium --port 4723 --allow-cors

:: Wait for Appium to start
timeout /t 10

:: Run tests
echo Running tests...
mvn clean test

pause
```

### Part 11: Useful ADB Commands

```cmd
# Device management
adb devices                          # List connected devices
adb kill-server                      # Kill ADB server
adb start-server                     # Start ADB server

# App management  
adb shell pm list packages           # List all packages
adb shell pm list packages -3        # List 3rd party packages
adb uninstall com.package.name       # Uninstall app
adb shell pm clear com.package.name  # Clear app data

# File operations
adb push local_file /sdcard/         # Push file to device
adb pull /sdcard/file local_path     # Pull file from device

# Logs
adb logcat                           # View device logs
adb logcat -c                        # Clear logs
adb logcat > logs.txt                # Save logs to file

# Screenshots
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png

# Input events
adb shell input tap 100 200          # Tap at coordinates
adb shell input text "Hello"         # Type text
adb shell input keyevent 3           # Press home button
```

### Verification Checklist

Run these commands to verify everything is set up correctly:

```cmd
# Check installations
java -version
mvn -version
node --version
npm --version
appium --version
adb --version
emulator -version

# Check environment variables
echo %ANDROID_HOME%
echo %JAVA_HOME%

# Run Appium doctor
appium-doctor --android
```

This comprehensive setup will give you a fully functional Android automation testing environment on Windows. You can now run your tests either through your IDE or via command line using Maven.
