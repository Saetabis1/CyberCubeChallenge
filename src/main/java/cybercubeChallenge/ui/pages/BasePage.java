package cybercubeChallenge.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class BasePage extends BasePageActions {

    @FindBy(css = "div[class='header-container-wrapper'] div[data-widget-type='logo']")
    public WebElement logo;

    public BasePage(WebDriver driver) {
        super(driver);
    }

}
