package framework.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class VacanciesPage extends BasePage {

    @FindBy(css = "select[id='filter-location']")
    public WebElement locationFilter;

    @FindBy(css = "select[id='filter-team']")
    public WebElement teamFilter;

    @FindBy(css = "ul[class='lever-group-items']")
    public List<WebElement> groupItems;

    @FindBy(css = "h3[class='lever-group-name']")
    public List<WebElement> groupTeams;

    @FindBy(css = "div[class='rf-lever-empty']")
    public WebElement noVacanciesMessage;

    public VacanciesPage(WebDriver driver) {

        super(driver);
        waitForAllElementsNoException(groupItems,1000);
    }

    public void filterLocation(String location){
        selectDropDownByValue(locationFilter, location);
    }

    public void filterTeam(String team){
        selectDropDownByValue(teamFilter, team);

    }


}
