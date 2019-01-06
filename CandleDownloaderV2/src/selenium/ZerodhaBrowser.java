package selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ZerodhaBrowser {
	public static String getRequestToken(String url) {
		System.setProperty("webdriver.chrome.driver", "c:\\chromedriver.exe");

		WebDriver driver = new ChromeDriver();

		driver.get(url);
		sleep(500);
		driver.manage().window().maximize();

		WebDriverWait wait = new WebDriverWait(driver, 60);// 1 minute

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input")));
		List<WebElement> loginPW = driver.findElements(By.cssSelector("input"));

		if(loginPW.size() > 2)
		{
			WebElement UserId = loginPW.get(0);
			UserId.sendKeys("BF2129");
			sleep(200);
			WebElement Password = loginPW.get(1);
			Password.sendKeys("bik@123");
			Password.sendKeys(Keys.ENTER);
			sleep(500);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input")));
			List<WebElement> secQs = driver.findElements(By.cssSelector("input"));

			for (WebElement secQ : secQs) {
				sleep(500);
				if (secQ.getAttribute("type").equals("password")) {
					String question = secQ.getAttribute("label");
					if (question.startsWith("What was the name of the college from which you graduated")) {
						secQ.sendKeys("nit");
					} else if (question.startsWith("Who is your spouse")) {
						secQ.sendKeys("goo");
					} else if (question.startsWith("What is the name of your first child")) {
						secQ.sendKeys("riy");
					} else if (question.startsWith("In Which bank did you first open your account")) {
						secQ.sendKeys("ici");
					}
					else if (question.startsWith("What is your moth")) {
						secQ.sendKeys("san");
					}
				}
			}
			if (secQs.size() > 1) {
				secQs.get(1).sendKeys(Keys.ENTER);
			}	
		}
		sleep(5000);
		String op = driver.getCurrentUrl();

		driver.quit();
		return op;
	}

	private static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
