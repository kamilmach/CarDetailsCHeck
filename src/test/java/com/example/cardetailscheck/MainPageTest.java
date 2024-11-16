package com.example.cardetailscheck;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainPageTest {
    String INPUT_FILE = "car_input.txt";
    String OUTPUT_FILE = "car_output.txt";
    String PAGE = "https://www.webuyanycar.com/";
    int TIMEOUT_MS = 2000;
    int WAIT_FOR_ELEMENT_S = 5;

    WebDriver driver;

    @BeforeClass
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.get(PAGE);
        waitForTheMainPageToLoad(driver);
        skipCookiePrompt(driver);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @DataProvider(name = "registrationNumbers")
    public Object[][] getRegistrationNumbers() throws IOException {
        List<String> carRegNumbers = carRegFromFile(INPUT_FILE);
        Object[][] data = new Object[carRegNumbers.size()][1];
        for (int i = 0; i < carRegNumbers.size(); i++) {
            data[i][0] = carRegNumbers.get(i);
        }
        return data;
    }


    @Test(dataProvider = "registrationNumbers")
    public void search(String regNumber) throws IOException, InterruptedException {
        Map<String, String[]> carDetails = carDetailsFromFile(OUTPUT_FILE);
        driver.get(PAGE);
        Thread.sleep(TIMEOUT_MS);
        enterRegNumber(driver, regNumber);
        enterRandomMileage(driver);
        clickValuation(driver);
        try {
            waitForDetailsPageToLoad(driver);
            Map<String, String[]> carDetailsFromPage = null;
            carDetailsFromPage = getDetails(driver, regNumber);
            String detailFromFile = Arrays.toString(carDetails.get(regNumber));
            String detailFromWeb = Arrays.toString(carDetailsFromPage.get(regNumber));
            System.out.println(Arrays.toString(carDetails.get(regNumber)));
            System.out.println(Arrays.toString(carDetailsFromPage.get(regNumber)));
            Assert.assertEquals(carDetails.get(regNumber), carDetailsFromPage.get(regNumber),
                    MessageFormat.format("Details Not Matching /n Car detail in file {0} /n " +
                            "car detail in web {1} ", detailFromFile, detailFromWeb));
        } catch (TimeoutException e) {
            Assert.fail(String.format("%s Car Reg doesn't exist", regNumber));
        }
        Thread.sleep(TIMEOUT_MS);
    }


    private List<String> carRegFromFile(String filePath) throws IOException {
        String ukCarRegPattern = "\\b[A-Z]{2}[0-9]{2}\\s?[A-Z]{3}\\b";
        Pattern pattern = Pattern.compile(ukCarRegPattern);

        List<String> regNumbers = new ArrayList<>();

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String carRegNumber = matcher.group();
                carRegNumber = carRegNumber.replaceAll("\\s", "");
                regNumbers.add(carRegNumber);
            }
        }
        return regNumbers;
    }

    private Map<String, String[]> carDetailsFromFile(String filePath) throws IOException {
        Map<String, String[]> carDetails = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] parts = line.split(",", 5);
                if (parts.length >= 4) {
                    String carRegNumber = parts[0];
                    carDetails.put(carRegNumber, Arrays.copyOfRange(parts, 1, parts.length));
                }
            }
        }
        return carDetails;
    }

    private void waitForTheMainPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_FOR_ELEMENT_S));
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[aria-label='Privacy']")));
    }

    private void skipCookiePrompt(WebDriver driver) {
        WebElement cookiePrompt = driver.findElement(By.cssSelector("[aria-label='Privacy']"));
        WebElement acceptCookiesBtn = cookiePrompt.findElement(By.cssSelector("[id^='onetrust-accept']"));
        acceptCookiesBtn.click();
    }

    private void enterRegNumber(WebDriver driver, String regNumber) {
        WebElement regNumberField = driver.findElement(By.cssSelector("[id='vehicleReg']"));
        regNumberField.sendKeys(regNumber);
    }

    private void enterRandomMileage(WebDriver driver) {
        WebElement Mileage = driver.findElement(By.cssSelector("[id='Mileage']"));
        Mileage.sendKeys(Integer.toString(randomMileage()));

    }

    private void clickValuation(WebDriver driver) {
        WebElement valuation = driver.findElement(By.cssSelector("[type='submit']"));
        valuation.click();
    }

    private void waitForDetailsPageToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_FOR_ELEMENT_S));
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[id='EmailAddress']")));
    }

    private Map<String, String[]> getDetails(WebDriver driver, String regNumber) {
        Map<String, String[]> carDetails = new HashMap<>();
        String Make = "";
        String Model = "";
        String Year = "";
        List<WebElement> details = driver.findElements(By.xpath("//div[contains(@class, 'details-vehicle-row')]"));
        for (int i = 0; i < details.size(); i++) {
            System.out.println(details.get(i).getText());
            if (details.get(i).getText().contains("Manufacturer:")) {
                Make = details.get(i).getText().split(": ")[1];
            }
            if (details.get(i).getText().contains("Model:")) {
                Model = details.get(i).getText().split(": ")[1];
            }
            if (details.get(i).getText().contains("Year:")) {
                Year = details.get(i).getText().split(": ")[1];
            }
        }
        ;
        String[] arr = new String[]{Make, Model, Year};
        carDetails.put(regNumber, arr);
        return carDetails;
    }

    private int randomMileage() {
        int min = 9000;
        int max = 180000;
        Random random = new Random();
        int randomNumber = min + random.nextInt(max - min + 1);
        return (int) (Math.round(randomNumber / 100.0) * 100);
    }


}
