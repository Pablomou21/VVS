package es.udc.paproject.e2etests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

public class AppTest {

    WebDriver driver;

    @BeforeEach
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    @Test
    public void testLogin() {

        login("testviewer", "pa2526");

    }

    @Test
    public void testViewSessionDetails() {
        login("testviewer", "pa2526");

        Select dateSelect = new Select(driver.findElement(By.id("billboardDate")));
        dateSelect.selectByIndex(1);

        WebElement tbody = driver.findElement(By.id("movies-tbody"));

        List<WebElement> divs = tbody.findElements(By.tagName("div"));
        WebElement firstMovie = divs.getFirst();
        String movieTitle = firstMovie.getText().trim();

        WebElement sessionsDiv = divs.get(1);
        List<WebElement> spans = sessionsDiv.findElements(By.tagName("span"));
        WebElement firstSession = spans.getFirst();
        String sessionTime = firstSession.getText().trim();
        firstSession.click();

        WebElement title = driver.findElement(By.id("movieTitle"));
        WebElement runtime = driver.findElement(By.id("runtime"));
        WebElement price = driver.findElement(By.id("price"));
        WebElement date = driver.findElement(By.id("date"));
        WebElement time = driver.findElement(By.id("time"));
        WebElement roomName = driver.findElement(By.id("roomName"));
        WebElement freeSeats = driver.findElement(By.id("freeSeats"));

        assertTrue(title.isDisplayed());
        assertTrue(runtime.isDisplayed());
        assertTrue(price.isDisplayed());
        assertTrue(date.isDisplayed());
        assertTrue(time.isDisplayed());
        assertTrue(roomName.isDisplayed());
        assertTrue(freeSeats.isDisplayed());

        assertTrue(title.getText().contains(movieTitle));
        assertTrue(time.getText().contains(sessionTime));

        WebElement buyTicketsForm = driver.findElement(By.id("buyTicketsForm"));
        assertTrue(buyTicketsForm.isDisplayed());
    }

    @Test
    public void testBuyTickets() {

        login("testviewer", "pa2526");

        driver.get("http://localhost:5173/catalog/session-details/5");

        WebElement movieTitle = driver.findElement(By.id("movieTitle"));
        String expectedMovieTitle = movieTitle.getText().trim();

        WebElement numTicketsInput = driver.findElement(By.id("numTickets"));
        numTicketsInput.clear();
        numTicketsInput.sendKeys("2");

        WebElement creditCardInput = driver.findElement(By.id("creditCardNum"));
        creditCardInput.sendKeys("2222222222222222");

        WebElement buyButton = driver.findElement(By.id("buyButton"));
        buyButton.click();

        WebElement purchaseOrderId = driver.findElement(By.id("purchaseOrderId"));
        String expectedOrderId = purchaseOrderId.getText().trim();

        WebElement ordersLink = driver.findElement(By.id("ordersLink"));
        ordersLink.click();

        WebElement firstOrderId = driver.findElement(By.id("orderId-0"));
        WebElement firstOrderMovieTitle = driver.findElement(By.id("orderMovieTitle-0"));

        assertEquals(expectedOrderId, firstOrderId.getText().trim());
        assertEquals(expectedMovieTitle, firstOrderMovieTitle.getText().trim());

    }

    @Test
    public void testDeliverTickets(){
        login("testticketseller", "pa2526");

        WebElement deliverLink = driver.findElement(By.id("deliverLink"));
        deliverLink.click();

        WebElement orderIdInput = driver.findElement(By.id("orderId"));
        orderIdInput.sendKeys("3");

        WebElement creditCardInput = driver.findElement(By.id("creditCardNum"));
        creditCardInput.sendKeys("1111111111111111");

        WebElement submitButton = driver.findElement(By.id("deliverSubmitButton"));
        submitButton.click();

        WebElement successMessage = driver.findElement(By.id("successMessage"));
        assertTrue(successMessage.isDisplayed());

        orderIdInput.clear();
        creditCardInput.clear();

        orderIdInput.sendKeys("3");
        creditCardInput.sendKeys("1111111111111111");

        submitButton.click();

        WebElement errorMessage = driver.findElement(By.id("errorMessage"));
        assertTrue(errorMessage.isDisplayed());

    }

    @AfterEach
    public final void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void login(String username, String password) {
        driver.get("http://localhost:5173");

        WebElement loginLink = driver.findElement(By.id("loginLink"));
        loginLink.click();

        WebElement usernameInput = driver.findElement(By.id("userName"));
        usernameInput.sendKeys(username);

        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.sendKeys(password);

        WebElement submitButton = driver.findElement(By.id("loginSubmitButton"));
        submitButton.click();

        WebElement userDropdown = driver.findElement(By.id("user-dropdown"));
        assertEquals(username.trim(), userDropdown.getText().trim());
    }

}