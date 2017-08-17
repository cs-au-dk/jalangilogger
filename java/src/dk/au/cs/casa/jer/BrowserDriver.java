package dk.au.cs.casa.jer;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class BrowserDriver {

    boolean driverAvailable = false;

    public void browse(URI uri, int pageLoadTimeout) {
        setDriverPath();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=800,600");
        options.addArguments("headless");
        options.addArguments("disable-gpu");
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        ChromeDriver driver = null;
        try {
            driver = new ChromeDriver(capabilities);
            driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout, TimeUnit.SECONDS);
            driver.manage().timeouts().implicitlyWait(pageLoadTimeout, TimeUnit.SECONDS);
            driver.manage().timeouts().setScriptTimeout(pageLoadTimeout, TimeUnit.SECONDS);

            driver.get(uri.toURL().toString());

            new WebDriverWait(driver, pageLoadTimeout).until((ExpectedCondition<Boolean>) wd -> {
                    Object result = ((JavascriptExecutor) wd).executeScript("return typeof window.J$ != 'undefined' && window.J$.stopBrowserInteraction");
                    return result != null && result.equals(true);
            });

            List<LogEntry> errors = driver.manage().logs().get(LogType.BROWSER).filter(Level.SEVERE);
            if(!errors.isEmpty()) {
                throw new RuntimeException("An error occurred in the browser:\n" + errors);
            }


        } catch (Exception e) {
            throw new RuntimeException("Unable to drive the browser", e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                }
                catch(Throwable e) {}
            }
        }
    }

    private void setDriverPath() {
        if (!driverAvailable) {
            String operatingSystem = System.getProperty("os.name");
            String executableName;
            if (operatingSystem.contains("Windows")) {
                executableName = "chromedriver-win.exe";
            } else if (operatingSystem.contains("Linux")) {
                executableName = "chromedriver-linux";
            } else if (operatingSystem.contains("Mac")) {
                executableName = "chromedriver-mac";
            } else {
                throw new RuntimeException("Unknown operating system: " + operatingSystem);
            }
            try {
                Path dir = Files.createTempDirectory("chromedriver");
                dir.toFile().deleteOnExit();
                Path destinationName = dir.resolve(executableName);
                System.setProperty("webdriver.chrome.driver", destinationName.toAbsolutePath().toString());
                try (InputStream is = this.getClass().getResourceAsStream("/webdriver/" + executableName)) {
                    Files.copy(is, destinationName, StandardCopyOption.REPLACE_EXISTING);
                    if (!destinationName.toFile().setExecutable(true))
                        throw new RuntimeException("Unable to set the driver executable");
                    destinationName.toFile().deleteOnExit();
                } catch (Throwable e) {
                    throw new RuntimeException("Unable to find webdriver resource " + executableName, e);
                }
            } catch (Throwable e) {
                throw new RuntimeException("Unable to make a suitable chrome driver executable location", e);
            }
        }
    }

    private static DesiredCapabilities buldCapabilities() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("window-size=400,400");
        options.addArguments("headless");
        options.addArguments("disable-gpu");
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        LoggingPreferences loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(LogType.BROWSER, Level.ALL);
        capabilities.setCapability(CapabilityType.LOGGING_PREFS, loggingPreferences);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        return capabilities;
    }
}

