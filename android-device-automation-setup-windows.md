# Android Emulator + Appium + UiAutomator2 Setup Guide for Windows

A complete step-by-step guide for setting up Android test automation environment on Windows machines.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation Steps](#installation-steps)
- [Environment Variables Setup](#environment-variables-setup)
- [Creating Android Virtual Device (AVD)](#creating-android-virtual-device-avd)
- [Launching Emulator & Appium](#launching-emulator--appium)
- [Test Connection](#test-connection)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)
- [Safely Closing Everything](#safely-closing-everything)

## Prerequisites

### Required Software
- Windows 10/11 (64-bit)
- At least 8GB RAM (16GB recommended)
- 10GB free disk space
- Virtualization enabled in BIOS

## Installation Steps

### 1. Install Java Development Kit (JDK)

```batch
# Download JDK 11 or higher from:
# https://www.oracle.com/java/technologies/downloads/

# Verify installation
java -version
javac -version
```

### 2. Install Android Studio

1. Download from: https://developer.android.com/studio
2. Run installer with default settings
3. Launch Android Studio once to complete initial setup
4. Open SDK Manager: `Tools → SDK Manager`
5. Install:
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools (latest)
   - Android Emulator
   - Android 13.0 (API 33) or higher

### 3. Install Node.js & NPM

```batch
# Download from: https://nodejs.org/ (LTS version)

# Verify installation
node --version
npm --version
```

### 4. Install Appium 2.x

```batch
# Clear npm cache and set registry
npm cache clean --force
npm config set registry https://registry.npmjs.org/

# Install Appium globally
npm install -g appium@latest

# Verify installation (should show 2.x.x)
appium --version
```

### 5. Install UiAutomator2 Driver

```batch
# Install driver
appium driver install uiautomator2

# Verify installation
appium driver list --installed
# Should show: uiautomator2@x.x.x [installed (npm)]
```

### 6. Install Appium Inspector (Optional)

Download from: https://github.com/appium/appium-inspector/releases

## Environment Variables Setup

### Step-by-step Configuration:

1. **Open System Environment Variables:**
   - Press `Win + X` → System → Advanced System Settings → Environment Variables

2. **Create/Update System Variables:**
   ```
   ANDROID_HOME = C:\Users\%USERNAME%\AppData\Local\Android\Sdk
   JAVA_HOME = C:\Program Files\Java\jdk-11
   ```

3. **Update PATH Variable (Add these entries):**
   ```
   %ANDROID_HOME%\platform-tools
   %ANDROID_HOME%\emulator
   %ANDROID_HOME%\cmdline-tools\latest\bin
   %JAVA_HOME%\bin
   ```

4. **Verify Setup (Open new CMD):**
   ```batch
   echo %ANDROID_HOME%
   echo %JAVA_HOME%
   adb --version
   emulator -version
   ```

## Creating Android Virtual Device (AVD)

### Method 1: Via Android Studio
1. Open Android Studio
2. Click "AVD Manager" icon
3. Click "Create Virtual Device"
4. Select Pixel 9 → Next
5. Download/Select system image (API 33 or higher)
6. Name it "Pixel_9" → Finish

### Method 2: Via Command Line

```batch
# List available system images
sdkmanager --list

# Download system image (Android 13)
sdkmanager "system-images;android-33;google_apis;x86_64"

# Create AVD
echo no | avdmanager create avd -n "Pixel_9" -k "system-images;android-33;google_apis;x86_64" -d "pixel"

# Verify AVD creation
emulator -list-avds
```

## Launching Emulator & Appium

### Quick Launch Commands

```batch
# Terminal 1: Start Emulator
emulator -avd Pixel_9

# Terminal 2: Verify device is connected
adb devices
# Should show: emulator-5554   device

# Terminal 3: Start Appium Server
appium --use-drivers=uiautomator2 -p 4723
```

### Automated Launch Script

Create `start-automation.bat`:

```batch
@echo off
echo ========================================
echo Starting Android Automation Environment
echo ========================================

:: Set paths
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator

:: Clean up existing instances
echo Cleaning up...
taskkill /F /IM "emulator.exe" 2>nul
adb kill-server 2>nul

:: Start ADB
echo Starting ADB server...
adb start-server

:: Launch emulator
echo Launching Pixel_9 emulator...
start "Emulator" emulator -avd Pixel_9

:: Wait for device
echo Waiting for emulator to boot...
adb wait-for-device

:WAIT_BOOT
timeout /t 3 /nobreak > nul
for /f "tokens=*" %%i in ('adb shell getprop sys.boot_completed 2^>nul') do set boot_completed=%%i
if not "%boot_completed%"=="1" goto WAIT_BOOT

echo Emulator is ready!
adb devices

:: Unlock screen
adb shell input keyevent 82

:: Start Appium
echo Starting Appium server...
start "Appium" cmd /k appium --use-drivers=uiautomator2 -p 4723

echo ========================================
echo Setup Complete!
echo Emulator: emulator-5554
echo Appium: http://localhost:4723
echo ========================================
pause
```

## Test Connection

### Sample Java Test Code

```java
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.URL;

public class ConnectionTest {
    public static void main(String[] args) throws Exception {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName("emulator-5554");
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage("com.android.settings");
        options.setAppActivity(".Settings");
        
        AndroidDriver driver = new AndroidDriver(
            new URL("http://localhost:4723"), options);
        
        System.out.println("Connected! Session: " + driver.getSessionId());
        Thread.sleep(3000);
        driver.quit();
    }
}
```

### Maven Dependencies

```xml
<dependency>
    <groupId>io.appium</groupId>
    <artifactId>java-client</artifactId>
    <version>9.2.0</version>
</dependency>
```

## Useful Commands

### ADB Commands

```batch
# List connected devices
adb devices

# Install APK
adb install path\to\app.apk

# Uninstall app
adb uninstall com.package.name

# Clear app data
adb shell pm clear com.package.name

# Take screenshot
adb exec-out screencap -p > screenshot.png

# Record screen (press Ctrl+C to stop)
adb shell screenrecord /sdcard/video.mp4
adb pull /sdcard/video.mp4

# Check current activity
adb shell dumpsys window | findstr "mCurrentFocus"

# Send text
adb shell input text "Hello World"

# Press back button
adb shell input keyevent 4

# Press home button
adb shell input keyevent 3
```

### Emulator Commands

```batch
# List AVDs
emulator -list-avds

# Launch with specific memory
emulator -avd Pixel_9 -memory 2048

# Launch in headless mode (no GUI)
emulator -avd Pixel_9 -no-window -no-audio

# Launch with specific port
emulator -avd Pixel_9 -port 5556

# Wipe user data
emulator -avd Pixel_9 -wipe-data
```

### Appium Commands

```batch
# Start with specific port
appium -p 4723

# Start with logging
appium --log-level debug --log appium.log

# List drivers
appium driver list

# Update driver
appium driver update uiautomator2

# Run with relaxed security
appium --relaxed-security --allow-insecure=adb_shell
```

## Troubleshooting

### Issue: "unknown command driver"
**Solution:** You have Appium 1.x installed. Uninstall and reinstall Appium 2.x:
```batch
npm uninstall -g appium
npm install -g appium@latest
appium driver install uiautomator2
```

### Issue: E401 Authentication Error
**Solution:** Clear npm registry and cache:
```batch
npm cache clean --force
npm config set registry https://registry.npmjs.org/
npm config delete _auth
npm install -g appium
```

### Issue: Emulator won't start
**Solution:** Check virtualization and AVD:
```batch
# Verify Intel HAXM is installed (for Intel CPUs)
sc query intelhaxm

# List AVDs
emulator -list-avds

# Try with more memory
emulator -avd Pixel_9 -memory 4096
```

### Issue: "Device not found" in ADB
**Solution:** Restart ADB:
```batch
adb kill-server
adb start-server
adb devices
```

### Issue: Appium can't find UiAutomator2
**Solution:** Reinstall driver:
```batch
appium driver uninstall uiautomator2
appium driver install uiautomator2
appium driver list --installed
```

### Issue: "ANDROID_HOME not set"
**Solution:** Verify environment variables:
```batch
# Check if set
echo %ANDROID_HOME%

# Set temporarily for current session
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator
```

### Run Diagnostics

Create `diagnose.bat`:

```batch
@echo off
echo === System Diagnostics ===
echo.
echo Java Version:
java -version 2>&1 | findstr version
echo.
echo Node Version:
node --version
echo.
echo NPM Version:
npm --version
echo.
echo Appium Version:
appium --version 2>nul || echo Appium not installed
echo.
echo Android Home:
echo %ANDROID_HOME%
echo.
echo ADB Version:
adb --version 2>nul || echo ADB not found
echo.
echo Emulator Version:
emulator -version 2>nul || echo Emulator not found
echo.
echo Available AVDs:
emulator -list-avds 2>nul || echo No AVDs found
echo.
echo Appium Drivers:
appium driver list --installed 2>nul || echo No drivers installed
echo.
pause
```

## Safely Closing Everything

### Graceful Shutdown Commands

```batch
# Close Appium (in Appium terminal, press Ctrl+C)

# Close emulator properly (choose one):
# Option 1: Via ADB
adb emu kill

# Option 2: Power off emulator
adb shell reboot -p

# Option 3: Via emulator command
adb shell input keyevent 26  # Press power button
adb shell input keyevent 66  # Press enter to confirm

# Kill ADB server
adb kill-server
```

### Complete Cleanup Script

Create `stop-automation.bat`:

```batch
@echo off
echo ========================================
echo Stopping Automation Environment
echo ========================================

echo Closing emulator gracefully...
adb emu kill 2>nul

timeout /t 3 /nobreak > nul

echo Stopping ADB server...
adb kill-server 2>nul

echo Terminating remaining processes...
taskkill /F /IM "emulator.exe" 2>nul
taskkill /F /IM "qemu-system-x86_64.exe" 2>nul
taskkill /F /IM "adb.exe" 2>nul
taskkill /F /IM "node.exe" /FI "WINDOWTITLE eq Appium*" 2>nul

echo.
echo ========================================
echo All processes stopped successfully!
echo ========================================
pause
```

## Quick Reference Card

| Task | Command |
|------|---------|
| Start Emulator | `emulator -avd Pixel_9` |
| Check Devices | `adb devices` |
| Start Appium | `appium -p 4723` |
| Install App | `adb install app.apk` |
| Take Screenshot | `adb exec-out screencap -p > screen.png` |
| Stop Emulator | `adb emu kill` |
| Kill ADB | `adb kill-server` |
| Unlock Screen | `adb shell input keyevent 82` |

## Additional Resources

- [Appium Documentation](http://appium.io/docs/en/latest/)
- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [UiAutomator2 Driver](https://github.com/appium/appium-uiautomator2-driver)
- [ADB Commands Reference](https://developer.android.com/studio/command-line/adb)

---

**Last Updated:** 2025  
**Tested on:** Windows 10/11, Appium 2.x, Android SDK 33+
