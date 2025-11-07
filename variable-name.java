import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


public abstract class BasePage {
    protected WebDriver driver;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface LocatorName {
        String value();
    }

    // ... your locatorNames cache + elementDisplayed method
}


public abstract class BasePage {
    protected WebDriver driver;
    private final Map<By, String> locatorNames = new HashMap<>();

    public BasePage(WebDriver driver) {
        this.driver = driver;
        cacheLocatorNames();
    }

    private void cacheLocatorNames() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(LocatorName.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(this);
                    if (value instanceof By) {
                        locatorNames.put((By) value, field.getAnnotation(LocatorName.class).value());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected WebElement elementDisplayed(By locator) {
        long start = System.nanoTime();

        WebElement element = driver.findElement(locator);
        boolean displayed = element.isDisplayed();

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;

        String name = locatorNames.getOrDefault(locator, "(unnamed)");
        System.out.println("Element: " + name + " | Displayed: " + displayed + " | Time: " + durationMs + " ms");

        return element;
    }
}


private final Map<String, By> locators = Map.of(
    "username", By.xpath("//input[@id='username']")
);

public WebElement getUsername() {
    return elementDisplayed("username");
}

public WebElement elementDisplayed(String name) {
    By locator = locators.get(name);
    WebElement element = driver.findElement(locator);
    boolean displayed = element.isDisplayed();
    System.out.println("Element: " + name + " | Displayed: " + displayed);
    return element;
}
