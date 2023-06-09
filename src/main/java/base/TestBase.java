package base;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import com.aventstack.extentreports.Status;
import io.github.bonigarcia.wdm.WebDriverManager;
import object.DriverDetails;
import object.HomePage;
import object.InfoPage;
import report.ExtentReport;
import util.Configuration;
import util.Key;
import static util.Key.*;
import static util.Browser.*;

public class TestBase extends ExtentReportListner{

	protected WebDriver driver;
	protected HomePage homePage;
	protected InfoPage infoPage;
	protected DriverDetails driverDetails;
	protected Configuration conf = new Configuration();

	@Parameters({BROWSER, "user", "access", "url", "os", "os-v", "bs-browser", "bs-browser-v"})
	@BeforeMethod
	public void beforeEachTest(@Optional(CHROME) String browserName, @Optional String user, @Optional String access, @Optional String bs_url, @Optional String osType,
			@Optional String osVer, @Optional String bsBrowser, @Optional String bsBrowserV) {
		// String browserName = conf.readProp(getValue(browser));
		initiatDriver(browserName, user, access, bs_url, osType, osVer, bsBrowser, bsBrowserV);
		initObject();
		driver.manage().window().maximize();
		int pageLoadTime = conf.readPropNum(getValue(pageLoad));
		int implicitLoadTime = conf.readPropNum(getValue(implicitLoad));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTime));
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitLoadTime));
		String urlName = conf.readProp(getValue(url));
		driver.get(urlName);
	}

	public void initiatDriver(String browser, String user, String access, String bs_url, String osType,
			String osVer, String bsBrowser, String bsBrowserV) {
		switch (browser) {
		case CHROME:
			WebDriverManager.chromedriver().setup();
			driver = new ChromeDriver();
			break;
		case FIREFOX:
			WebDriverManager.firefoxdriver().setup();
			driver = new FirefoxDriver();
			break;
		case EDGE:
			WebDriverManager.edgedriver().setup();
			driver = new EdgeDriver();
			break;
		case SAFARI:
			WebDriverManager.safaridriver().setup();
			driver = new SafariDriver();
			break;
		case REMOTE:
			driver = setRemoteDriver(user, access, bs_url, osType, osVer, bsBrowser, bsBrowserV);
			break;
		default:
			WebDriverManager.chromedriver().setup();
			driver = new ChromeDriver();
			break;
		}
	}

	protected void initObject() {
		homePage = new HomePage(driver);
		infoPage = new InfoPage(driver);
		driverDetails = new DriverDetails(driver);
	}

	private String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
	
	@AfterMethod
	public void aftereEachTest(ITestResult result, Method method) {
		for(String group : result.getMethod().getGroups()) {
			ExtentReport.getTest().assignCategory(group);
		}
		
		if(result.getStatus() == ITestResult.SUCCESS) {
			ExtentReport.getTest().log(Status.INFO, "Test Passed");
		}else if(result.getStatus() == ITestResult.SKIP) {
			ExtentReport.getTest().log(Status.SKIP, "Test Skipped");
		}else if(result.getStatus() == ITestResult.FAILURE) {
			ExtentReport.getTest().log(Status.FAIL, "Test Failed");
			ExtentReport.getTest().log(Status.FAIL, getStackTrace(result.getThrowable()));
			ExtentReport.getTest().addScreenCaptureFromPath(captureScreenShot(method, driver));
		}
	}
	
	@AfterMethod
	public void closingBrowser() {
		driver.quit();
	}

	private String getValue(Key key) {
		return key.name();
	}

	public WebDriver setRemoteDriver(String user, String access, String bs_url, String osType,
			String osVer, String bsBrowser, String bsBrowserV) {
		MutableCapabilities capabilities = new MutableCapabilities();
		capabilities.setCapability(getValue(browserName), bsBrowser);
		capabilities.setCapability(getValue(browserVersion), bsBrowserV);
		HashMap<String, Object> browserstackOptions = new HashMap<String, Object>();
		browserstackOptions.put(getValue(os), osType);
		browserstackOptions.put(getValue(osVersion), osVer);
		capabilities.setCapability(BS_OPT, browserstackOptions);
		
		String urlString = "https://" + user + ":" + access + bs_url;
		
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
        return new RemoteWebDriver(url, capabilities);
	}
}
