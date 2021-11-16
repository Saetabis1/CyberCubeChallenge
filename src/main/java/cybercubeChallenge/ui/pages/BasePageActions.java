package cybercubeChallenge.ui.pages;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import cybercubeChallenge.ui.common.ResourceNotFoundException;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.util.Preconditions.checkArgument;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

@Log4j
public class BasePageActions {

    protected FluentWait<WebDriver> fluentWait;
    protected WebDriver driver;
    protected String url;
    protected String originalWindowHandle;
    protected static final int TIMEOUT = 60;

    @SuppressWarnings({"unchecked"})
    public BasePageActions(WebDriver driver) {

        this.driver = driver;
        PageFactory.initElements(driver, this);
        originalWindowHandle = driver.getWindowHandle();
    }

    public String getBodyText(){
        return driver.findElement(By.tagName("body")).getText();
    }

    public String displayAttribute(String elementAttribute) {
        String returnValue;
        try {
            if (elementAttribute.contains("Proxy element for"))
                returnValue = elementAttribute;
            if (elementAttribute.contains("By.AccessibilityId")) {
                returnValue = elementAttribute.split("By.AccessibilityId: ")[1];
                returnValue = returnValue.split("}")[0];
            } else if (elementAttribute.contains("By.xpath") && elementAttribute.contains("By.chained")) {
                returnValue = elementAttribute.split("By.xpath: ")[1];
                returnValue = returnValue.split("}")[0];
            } else {
                String[] strArray = elementAttribute.split(" -> ")[1].replace("]", "").split(": ");
                returnValue = strArray[1];
            }
            returnValue = returnValue.replace("[data-auto=", "");
            returnValue = returnValue.replace("By.cssSelector:", "");
            returnValue = returnValue.replace("//button[@type=", "");
        } catch (Exception e) {
            return elementAttribute;
        }
        return returnValue;
    }

    public void sendKeys(WebElement elementAttribute, Keys keyValue) {
        elementAttribute.sendKeys(keyValue);
        logSendKeys(elementAttribute, keyValue.toString());
    }

    public void sendKeys(By element, String keyValue) {
        sendKeys(driver.findElement(element), keyValue);
    }

    public void sendKeys(WebElement elementAttribute, String elementValue) {
        scrollIntoElement(elementAttribute);
        elementAttribute.clear();
        elementAttribute.sendKeys(elementValue);
        logSendKeys(elementAttribute, elementValue);
    }

    public void sendKeysOnReactElement(WebElement elementAttribute, String elementValue) {
        URL url = Resources.getResource("js/react-set-value.js");
        String jsString = "";
        try {
            jsString = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            log.info("Custom js file read failed", e);
        }
        JavascriptExecutor executor = ((JavascriptExecutor) driver);
        executor.executeScript(jsString);
        executor.executeScript("reactSetValue(arguments[0], arguments[1]);", elementAttribute, elementValue);
    }

    public void sendKeysEncrypt(WebElement elementAttribute, String elementValue) {
        scrollIntoElement(elementAttribute);
        elementAttribute.clear();
        elementAttribute.sendKeys(elementValue);
        log.info("Entered text **************" + " in " + displayAttribute(elementAttribute.toString()));
    }

    private void logSendKeys(WebElement elementAttribute, String elementValue) {
        if (displayAttribute(elementAttribute.toString()).contains("password"))
            log.info("Entered text **************" + " in " + displayAttribute(elementAttribute.toString()));
        else
            log.info("Entered text " + elementValue + " in " + displayAttribute(elementAttribute.toString()));
    }

    public void cleanElementValue(WebElement element) {
        Integer valueLength = element.getAttribute("value").length();
        for(int i = 0; i < valueLength; i++) {
            element.sendKeys(Keys.BACK_SPACE);
        }
    }

    public void cleanElementValue(By element) {
        cleanElementValue(driver.findElement(element));
    }

    public void forceCleanUpAndSendKeys(WebElement elementAttribute, String elementValue) {
        cleanElementValue(elementAttribute);
        sendKeys(elementAttribute, elementValue);
    }

    public void forceCleanUpAndSendKeys(By element, String elementValue) {
        forceCleanUpAndSendKeys(driver.findElement(element), elementValue);
    }

    public void hoverOnElement(WebElement element) {
        String elementStr = element.toString();
        Actions action = new Actions(driver);
        action.moveToElement(element).perform();
        log.info("Hover on element " + displayAttribute(elementStr));
    }

    public void click(WebElement element) {
        String elementStr = element.toString();
        log.info("Clicking on element " + displayAttribute(elementStr));

        try {
            element.click();
            log.info("Clicked on : " + displayAttribute(elementStr));
        } catch (WebDriverException e) {
            //scroll into view and click
            scrollIntoElement(element);
            try {
                element.click();
            } catch (WebDriverException te) {
                //scroll down and click
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0,250)", "");
                try {
                    element.click();
                } catch (WebDriverException tte) {
                    //scroll up and click
                    ((JavascriptExecutor) driver).executeScript("scroll(0,-250);");
                    try {
                        element.click();
                    } catch (WebDriverException ttte) {
                        new Actions(driver).moveToElement(element).perform();
                        try {
                            element.click();
                        } catch (WebDriverException tttte) {
                            javaScriptClick(element);
                        }
                    }
                }
            }
        }
    }

    public void clickUntilElementIsShowed(WebElement elementToClick,By elementToBeShowed){
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
        new WebDriverWait(driver, 200)
                .ignoring(NoSuchElementException.class,StaleElementReferenceException.class)
                .pollingEvery(Duration.ofSeconds(2))
                .until(driver -> {
                    if (isElementPresent(elementToBeShowed)) {
                        return true;
                    }
                    click(elementToClick);
                    return isElementPresent(elementToBeShowed);
                });
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
    }

    public void selectCheckBox(boolean select, WebElement element) {
        checkArgument(element != null, "Element cant be null");
        String elementStr = element.toString();
        if ((select && element.isSelected()) || (!select && !element.isSelected())) return;
        // Sometimes click fails silently for checkboxes, in that case we try javascript
        click(element);
        if (element.isSelected() != select) {
            javaScriptClick(element);
        }
        if (element.isSelected() != select) {
            throw new RuntimeException("Couldn't change the checkbox state:" + elementStr);
        }
    }

    public void javaScriptClick(WebElement element){
        // Must get the info before clicking on the element otherwise if page changes reference to element will be lost
        // and takes long time to retrieve the info
        String elementInfo = element.toString();
        JavascriptExecutor executor = ((JavascriptExecutor) driver);
        executor.executeScript("arguments[0].scrollIntoView(true);", element);
        executor.executeScript("arguments[0].click();", element);
        log.info("Clicked on : " + displayAttribute(elementInfo) + " using javascript");
    }

    public void javaScriptClick(By by) {
        WebElement element = driver.findElement(by);
        javaScriptClick(element);
    }

    public void javaScriptFocusOnElement(WebElement element){
        String elementInfo = element.toString();
        JavascriptExecutor executor = ((JavascriptExecutor) driver);
        executor.executeScript("arguments[0].focus();", element);
        log.info("Focused on : " + displayAttribute(elementInfo) + " using javascript");
    }

    public void javaScriptFocusOnElement(By element){
        javaScriptFocusOnElement(driver.findElement(element));
    }


    /**
     * works like `driver.findElement()' but is faster
     */
    public WebElement javaScriptFindElement(By by) {
        String[] bySplitted = by.toString().split(": ", 2);
        String locatorType = bySplitted[0];
        String locator = bySplitted[1];

        WebElement element;
        JavascriptExecutor js = (JavascriptExecutor) driver;
        switch(locatorType) {
            case ("By.xpath"):
                element = (RemoteWebElement) js.executeScript(String.format("return document.evaluate(\"%s\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue", locator));
                break;
            case ("By.id"):
                element = (RemoteWebElement) js.executeScript(String.format("return document.getElementById(\"%s\")", locator));
                break;
            case ("By.cssSelector"):
                element = (RemoteWebElement) js.executeScript(String.format("return document.querySelector(\"%s\")", locator));
                break;

            default:
                throw new RuntimeException("Could not identify locator type '" + locator +"'. Please add if needed");
        }

        if (element == null) throw new RuntimeException("Could not locate element " + by);

        return element;
    }

    public void click(By by){
        click(driver.findElement(by));
    }

    public void blur(){
        ((JavascriptExecutor) driver).executeScript("!!document.activeElement ? document.activeElement.blur() : 0");
    }

    public boolean isElementPresent(By by) {
        checkArgument(by != null, "Locator cant be null");
        List<WebElement> elements = driver.findElements(by);
        return 0 != elements.size();
    }

    public int elementCount(By element) {
        return driver.findElements(element).size();
    }

    public boolean isElementPresent(WebElement element) {
        checkArgument(element != null, "Web element can't be null");
        List<WebElement> elements = driver.findElements(returnElement(element.toString()));
        return 0 != elements.size();
    }

    public boolean isElementNotPresent(By by) {
        checkArgument(by != null, "Locator can't be null");
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        List<WebElement> elements = driver.findElements(by);
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
        return 0 == elements.size();
    }

    public boolean isElementNotPresent(WebElement element) {
        checkArgument(element != null, "Web element can't be null");
        return isElementNotPresent(returnElement(element.toString()));
    }

    public boolean isElementNotPresentWithTimeout(WebElement element, int timeOut) {
        checkArgument(element != null, "Web element can't be null");
        driver.manage().timeouts().implicitlyWait(timeOut, TimeUnit.SECONDS);
        boolean isElementNotFound = isElementNotPresent(returnElement(element.toString()));
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
        return isElementNotFound;
    }

    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    public boolean checkAbsenceOfAllElements(List<WebElement> elements) {
        checkArgument(elements != null && !elements.isEmpty(), "You must provide the elements");
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        for (WebElement element : elements) {
            if (driver.findElements(returnElement(element.toString())).size() > 0) {
                driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
                return false;
            }
        }
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
        return true;
    }

    private By returnElement(String stringElement) {
        if (stringElement == null) {
            return null;
        }

        if (stringElement.contains("->")) {
            String[] splitElement = stringElement.split(" -> ")[1].split(": ");

            String locatorType = splitElement[0];
            String temp = splitElement[1];

            switch (locatorType) {
                case "partial link text":
                    locatorType = "partialLinkText";
                    break;
                case "link text":
                    locatorType = "linkText";
                    break;
                case "css selector":
                    locatorType = "cssSelector";
                    break;
            }

            String locatorPath = temp.substring(0, temp.length() - 1);
            return returnElement(locatorType, locatorPath);

        } else {
            int splitIndex = 4;
            String[] splitElement = stringElement.split(" ");
            String locatorType = splitElement[splitIndex].substring(splitElement[splitIndex].lastIndexOf(".") + 1,
                    splitElement[splitIndex].lastIndexOf(":"));
            int index = splitElement[splitIndex + 1].length();
            String locatorPath = splitElement[splitIndex + 1].substring(0, index - 1);
            return returnElement(locatorType, locatorPath);
        }
    }

    public void takeScreenshot(String fileName) {

        TakesScreenshot scrShot =((TakesScreenshot)driver);

        File scrFile = scrShot.getScreenshotAs(OutputType.FILE);

        String userDirectory = Paths.get("")
                .toAbsolutePath() +"\\"+ fileName + ".png";

        log.info(userDirectory);

        try {
            FileUtils.copyFile(scrFile, new File(userDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void setToMobileSize() {

    }

    private enum Locator {
        xpath, cssSelector, id, tagName, name, linkText, partialLinkText, className
    }

    // Refreshing DOM Elements
    private By returnElement(String locatorType, String locatorPath) {

        Locator locator = Locator.valueOf(locatorType);

        switch (locator) {

            case id:
                return By.id(locatorPath);

            case xpath:
                return By.xpath(locatorPath);

            case name:
                return By.name(locatorPath);

            case className:
                return By.className(locatorPath);

            case cssSelector:
                return By.cssSelector(locatorPath);

            case linkText:
                return By.linkText(locatorPath);

            case tagName:
                return By.tagName(locatorPath);

            case partialLinkText:
                return By.partialLinkText(locatorPath);

            default:
                throw new RuntimeException("Unknown locator " + locatorType + " : " + locatorPath);
        }
    }

    public void navigateTo(String toURL) {
        log.info("Navigate to - " + toURL);
        driver.navigate().to(toURL);
    }

    public void presenceOfElementLocated(WebElement element) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(returnElement(element.toString())));
    }

    public void presenceOfElementLocated(By element) {
        presenceOfElementLocated(element, TIMEOUT);
    }

    public void presenceOfElementLocated(By element, int timeoutInSeconds) {
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        new WebDriverWait(driver, timeoutInSeconds)
                .until(ExpectedConditions.presenceOfElementLocated(element));
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
    }

    public void visibilityOfElementLocated(By element) {
        visibilityOfElementLocated(element,TIMEOUT);
    }

    public void visibilityOfElementLocated(WebElement element) {
        visibilityOfElementLocated(element,TIMEOUT);
    }

    public void visibilityOfElementLocated(By by, int timeoutInSeconds) {
        checkArgument(by != null, "By cant be null");
        new WebDriverWait(driver, timeoutInSeconds)
                .ignoring(WebDriverException.class)
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    public void waitForElementClickable(By by, int timeoutInSeconds) {
        checkArgument(by != null, "By cant be null");
        new WebDriverWait(driver, timeoutInSeconds)
                .ignoring(WebDriverException.class)
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    public void waitForElementClickable(By by) {
        waitForElementClickable(by, TIMEOUT);
    }

    public boolean isElementClickable(By by){
        try{
            waitForElementClickable(by, TIMEOUT);
            return true;
        }catch (TimeoutException ex){
            return false;
        }
    }

    public void waitForElementClickable(WebElement element, int timeoutInSeconds) {
        checkArgument(element != null, "By cant be null");
        new WebDriverWait(driver, timeoutInSeconds)
                .ignoring(WebDriverException.class)
                .until(ExpectedConditions.elementToBeClickable(element));
    }

    public void waitForElementClickable(WebElement element) {
        waitForElementClickable(element, TIMEOUT);
    }

    public void visibilityOfElementLocated(WebElement element, int timeoutInSeconds) {
        checkArgument(element != null, "Element cant be null");
        new WebDriverWait(driver, timeoutInSeconds)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.visibilityOf(element));
    }

    public void elementTextPopulated(WebElement element, int timeoutInSeconds) {
        checkArgument(element != null, "Element cant be null");
        new WebDriverWait(driver, timeoutInSeconds)
                .until(elementTextIsNotEmpty(element));
    }

    public void invisibilityOfElement(By element){
        new WebDriverWait(driver, TIMEOUT)
                .ignoring(WebDriverException.class)
                .until(invisibilityOfElementLocated(element));
    }
    public void stalenessOfElement(WebElement element, int timeoutInSeconds){
        new WebDriverWait(driver, timeoutInSeconds)
                .until(ExpectedConditions.stalenessOf(element));
    }

    public void invisibilityOfElement(WebElement element){
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.invisibilityOf(element));
    }

    public void textToBePresentInElement(By element, String text) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.textToBePresentInElementLocated(element, text));
    }

    public void waitForElementToHaveText(By element) {
        new WebDriverWait(driver, TIMEOUT)
                .until((ExpectedCondition<Boolean>) driver -> driver.findElement(element).getText().length() != 0);
    }

    public void numberOfElementsToBe(By element, int numOfElements) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.numberOfElementsToBe(element, numOfElements));
    }

    /**
     * Clicks on first element passed, switch to new window, validate text exist in given element
     * Please avoid #elementLocator to be a Menu element or any element that can load first before the rest of the page
     * @param link2Click element to click for new window to open(usually is a link)
     * @param string2Search text to validate opened window has
     * @param elementLocator element that supposed to contain text validation
     */
    public void validateTextElementInNewWindow(WebElement link2Click, List<String> string2Search, By elementLocator) {
        originalWindowHandle = driver.getWindowHandle();
        click(link2Click);
        log.info("Title of original Page:" + driver.getTitle());

        Set<String> windowHandles = driver.getWindowHandles();

        for (String window : windowHandles) {
            if (!window.equals(originalWindowHandle)) {
                driver.switchTo().window(window);
                url = driver.getCurrentUrl();
                log.info("current url is: " + url);
                waitForPageLoad();
                break;
            }
        }
        waitForElementToHaveText(elementLocator);
        String elementText = driver.findElement(elementLocator).getText();

        driver.close();
        driver.switchTo().window(originalWindowHandle);
        for (String s:string2Search){
            Assertions.assertThat(elementText)
                    .as("Element does not contain expected text")
                    .containsIgnoringCase(s);
        }
    }

    public void validateTextElementInNewWindow(By element, String string2Search, By elementLocator) {
        validateTextElementInNewWindow(driver.findElement(element), string2Search, elementLocator);
    }

    public void validateTextElementInNewWindow(WebElement element, String string2Search, By elementLocator) {
        validateTextElementInNewWindow(element, List.of(string2Search), elementLocator);
    }

    public void implicitlyWait(int TIMEOUT){
        driver.manage().timeouts().implicitlyWait(TIMEOUT, TimeUnit.SECONDS);
    }

    public void fluentWait(WebElement element) {
        fluentWait(returnElement(element.toString()));
    }

    public void waitForElementToBeDisplayed(By by, int timeout, int pollInterval) {
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofSeconds(pollInterval))
                .ignoring(NoSuchElementException.class)
                .until(driver -> driver.findElement(by).isDisplayed());
    }

    public void fluentWait(By by) {

        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(TIMEOUT))
                .pollingEvery(Duration.ofSeconds(2))
                .ignoring(NoSuchElementException.class);

        try {
            wait.until(driver -> driver.findElement(by));
        } catch (TimeoutException e) {
        }
    }

    public String getCookieValue(String cookie) {
        return driver.manage().getCookieNamed(cookie).getValue();
    }

    public boolean isCookiePresent(String cookieName)
    {
        checkArgument(cookieName != null && !cookieName.isEmpty(),
                "Cookie name cannot be null or empty");
        return driver.manage().getCookieNamed(cookieName) != null;
    }

    public void selectDropDownByText(WebElement selectElement, String text){
        Select dropdown = new Select(selectElement);
        dropdown.selectByVisibleText(text);
        log.info("Selected '" + text + "' from the dropdown");
    }

    public void selectDropDownByValue(WebElement selectElement, String value) {
        checkArgument(selectElement != null, "Select element cannot be null");
        checkArgument(value != null, "Value cannot be null");

        waitForElementClickable(selectElement, 2000);
        Select dropdown = new Select(selectElement);
        dropdown.selectByValue(value);
        log.info("Selected value '" + value+ "' from the dropdown");
    }

    public void selectDropDownByTextIgnoringCase(WebElement selectElement, String text) {
        checkArgument(selectElement != null && text != null && !text.isEmpty(), "You must provide all the values");
        Select dropdown = new Select(selectElement);
        for (WebElement option : dropdown.getOptions()) {
            if (option.getText().equalsIgnoreCase(text)) {
                dropdown.selectByVisibleText(option.getText());
                break;
            }
        }
        log.info("Selected " + text + " from the dropdown");
    }

    public void selectDropDownByValue( By by, String text) {
        selectDropDownByValue(driver.findElement(by), text);
    }

    public void pause(Integer seconds){}

    public void switchToActiveWindow() {
        pause(2);
        //originalWindowHandle = driver.getWindowHandle();
        Set<String> windowHandles = driver.getWindowHandles();
        for (String wHandle : windowHandles) {
            if (!wHandle.equals(originalWindowHandle)) {
                driver.switchTo().window(wHandle);
            }
        }
    }

    public void switchToOriginalWindow() {
        pause(2);
        driver.switchTo().window(originalWindowHandle);
    }

    protected void loadPage(List<WebElement> mandatoryElements) {
        //Wait for dom to go to ready state
        WebDriverWait wait = new WebDriverWait(driver,TIMEOUT);

        waitForPageLoad();
        // Wait fo mandatory elements to be visible
        implicitlyWait(2);
        for (WebElement elm : mandatoryElements) {
            log.info("Looking for mandatory elements on the page : " + elm);
            try {
                // Wait for element to be found and visible
                wait
                        .ignoring(StaleElementReferenceException.class,NoSuchElementException.class)
                        .pollingEvery(Duration.ofSeconds(5))
                        .until(visibilityOf(elm));
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        implicitlyWait(TIMEOUT);
        //Todo Wait for overlay load window to disappear

    }

    public ExpectedCondition<WebElement> visibilityOf(final WebElement element) {
        return new ExpectedCondition<>() {
            public WebElement apply(WebDriver driver) {
                scrollIntoElement(element);
                return element.isDisplayed() ? element : null;
            }

            public String toString() {
                return "Couldnt verify visibility of the element: " + element;
            }
        };
    }

    /**
     * Use navigateToPage method for classes that are annotated with PageProperties
     */
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

    public <T extends BasePageActions> T navigateToPage(String url, Class<T> page) {
        checkArgument(url != null && !url.isEmpty(), "URL cant be null or empty");
        checkArgument(page != null, "next page cant be null");
        navigateTo(url);
        return getPage(page);
    }

    public WebElement getElement(By by){
        return driver.findElement(by);
    }

    public List<WebElement> getElements(By by){
        return driver.findElements(by);
    }

    public String getH1Tag(){
        return driver.findElement(By.tagName("h1")).getText();
    }

    public void waitForAttributeValueToChange(By by, String attribute, String expectedAttributeValue) {
        checkArgument(attribute != null && !attribute.isEmpty(), "attribute cant be null or empty");
        checkArgument(expectedAttributeValue != null && !expectedAttributeValue.isEmpty(),
                "attribute value cant be null or empty");
        new WebDriverWait(driver, TIMEOUT)
                .ignoring(WebDriverException.class)
                .until(ExpectedConditions.attributeToBe(by, attribute, expectedAttributeValue));
    }

    public void waitForPageLoad() {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
    }

    public void scrollIntoElement(WebElement element){

        ((JavascriptExecutor) driver)
                .executeScript("var rectSize = arguments[0].getBoundingClientRect();" +
                        "var elementTop = rectSize.top + window.pageYOffset;" +
                        "var middle = elementTop - (window.innerHeight / 2);" +
                        "window.scrollTo(0, middle);", element);
    }


    public void waitForElementToContainsText(WebElement element, String text) {
        new WebDriverWait(driver, TIMEOUT)
                .until(ExpectedConditions.textToBePresentInElement(element, text));
    }

    public void waitForElementNoException(WebElement webElement, int timeout) {
        FluentWait<WebDriver> wait = new WebDriverWait(driver, timeout)
                .ignoring(NoSuchElementException.class);
        try {
            wait.until(ExpectedConditions.visibilityOf(webElement));
        } catch (TimeoutException | NoSuchElementException e) {
            log.info("Can't verify visibility of the element: " + e);
        }
    }

    public void waitForAllElementsNoException(List<WebElement> elements, int timeout) {
        FluentWait<WebDriver> wait = new WebDriverWait(driver, timeout)
                .ignoring(NoSuchElementException.class);
        try {
            wait.until(ExpectedConditions.visibilityOfAllElements(elements));
        } catch (TimeoutException | NoSuchElementException e) {
            log.info("Can't verify visibility of the elements: " + e);
        }
    }


    private static ExpectedCondition<Boolean> elementTextIsNotEmpty(final WebElement element) {
        return new ExpectedCondition<>() {
            public Boolean apply(WebDriver driver) {
                try {
                    return !element.getText().isEmpty();
                } catch (StaleElementReferenceException var3) {
                    return null;
                }
            }

            public String toString() {
                return "element text cannot be empty";
            }
        };
    }

    public <T extends BasePageActions> T refreshPage(Class<T> page) {
        driver.navigate().refresh();
        return getPage(page);
    }

    public <T extends BasePageActions> T refreshPage(T page) {
        driver.navigate().refresh();
        return page;
    }

    public void enableCustomToggle(List<WebElement> elements, Boolean enable) {
        WebElement toggle = enable ? elements.get(0) : elements.get(1);
        if (StringUtils.isNotEmpty(toggle.getAttribute("aria-pressed")) &&
                !toggle.getAttribute("aria-pressed").equalsIgnoreCase("true")) {
            click(toggle);
        }
    }

    public String getElementText(By by){
        return driver.findElement(by).getText();
    }
    public String getAttributeValue(By by){
        return driver.findElement(by).getAttribute("value");
    }

    public String getCurrentPageUrl(){
        return driver.getCurrentUrl();
    }
    public Logger getLogger(){ return log; }

    protected void waitForLoading()
    {
        fluentWait.until(ExpectedConditions.attributeToBe(By.cssSelector("div[data-delay=\"250\"]"),"z-index","100"));
        fluentWait.until(ExpectedConditions.attributeToBe(By.cssSelector("div[data-delay=\"250\"]"),"z-index","0"));
    }
}
