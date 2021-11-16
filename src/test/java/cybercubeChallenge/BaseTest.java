package cybercubeChallenge;

import cybercubeChallenge.api.Services;
import cybercubeChallenge.ui.common.PagesConfig;
import cybercubeChallenge.ui.common.ResourceNotFoundException;
import cybercubeChallenge.ui.pages.BasePageActions;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.log4j.Log4j;
import org.apache.log4j.PropertyConfigurator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.BeforeSuite;
import org.testng.asserts.SoftAssert;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Log4j
public class BaseTest {

    public static SoftAssert softly;

    protected static PagesConfig pagesConfig;

    protected WebDriver driver;

    protected static Services api;

    @BeforeSuite
    public static void setupClass() {

        WebDriverManager.chromedriver().setup();
        softly = new SoftAssert();
        pagesConfig = new PagesConfig();
        api = new Services();
        setLog4j();
    }

    public <T extends BasePageActions> T navigateToPage(String url, Class<T> page) {
        checkArgument(url != null && !url.isEmpty(), "URL cant be null or empty");
        checkArgument(page != null, "next page cant be null");
        navigateTo(url);
        return getPage(page);
    }

    public void navigateTo(String server) {
        log.info("Navigate to - " + server);
        driver.navigate().to(server);

    }

    public  <T extends BasePageActions> T getPage(Class<T> page) {
        if (page == null) {
            return null;
        }
        try {
            log.info("Current URL: " + driver.getCurrentUrl() + " Corresponding Page Object: " + page.getSimpleName());
            return page.getConstructor(WebDriver.class).newInstance(driver);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ResourceNotFoundException("Couldn't instantiate object from class: " + page.getSimpleName() + ", current URL: " + driver.getCurrentUrl(), e);
        }
    }

    public static void setLog4j(){
        PropertyConfigurator.configure(BaseTest.class
                .getClassLoader().getResourceAsStream("log4j.properties"));
    }
}
