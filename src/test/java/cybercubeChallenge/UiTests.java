package cybercubeChallenge;

import cybercubeChallenge.ui.pages.*;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class UiTests extends BaseTest {

    @BeforeMethod
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    @AfterMethod
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void searchJobByLocation() {
        VacanciesPage vacanciesPage = navigateToPage(pagesConfig.getVacancies(), VacanciesPage.class);

        vacanciesPage.filterLocation("Tallinn");

        vacanciesPage.groupItems.forEach(item ->
                assertEquals(item.findElement(By.cssSelector("span[class='lever-job-location location']"))
                        .getText(), "Tallinn"));
    }

    @Test
    public void searchJobByTeam() {
        VacanciesPage vacanciesPage = navigateToPage(pagesConfig.getVacancies(), VacanciesPage.class);

        vacanciesPage.filterTeam("Operations");

        vacanciesPage.groupItems.forEach(item ->
                assertEquals(item.findElement(By.cssSelector("span[class='lever-job-team']"))
                        .getText(), "Operations"));

        assertEquals(vacanciesPage.groupTeams.size(), 1);
    }

    @Test
    public void searchJobByLocationAndTeam() {
        VacanciesPage vacanciesPage = navigateToPage(pagesConfig.getVacancies(), VacanciesPage.class);

        vacanciesPage.filterLocation("Tallinn");
        vacanciesPage.filterTeam("Operations");

        vacanciesPage.groupItems.forEach(item ->
                assertEquals(item.findElement(By.cssSelector("span[class='lever-job-location location']"))
                        .getText(), "Tallinn"));

        vacanciesPage.groupItems.forEach(item ->
                assertEquals(item.findElement(By.cssSelector("span[class='lever-job-team']"))
                        .getText(), "Operations"));

        assertEquals(vacanciesPage.groupTeams.size(), 1);
    }

    @Test
    public void searchJobByLocationAndTeamWithoutEntries() {
        VacanciesPage vacanciesPage = navigateToPage(pagesConfig.getVacancies(), VacanciesPage.class);

        vacanciesPage.filterLocation("Tallinn");
        vacanciesPage.filterTeam("Client Success");

        assertEquals(vacanciesPage.groupItems.size(), 0);

        assertEquals(vacanciesPage.groupTeams.size(), 0);

        assertEquals(vacanciesPage.noVacanciesMessage.getText(), "There are no jobs matching this criteria, please try resetting the filters above.");
    }
}
