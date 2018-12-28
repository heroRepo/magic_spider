package com.testspider.downloaders;

import com.testspider.utils.ChromeDriverLocator;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

public class ChromePageDownLoader implements Downloader {
  private static final Integer LoadTimeout = 5000;

  public ChromePageDownLoader() {
    System.setProperty("webdriver.chrome.driver", ChromeDriverLocator.getChromeDriverPath());
  }

  @Override
  public Page download(Request request, Task task) {
    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--headless");
    chromeOptions.addArguments("disable-gpu");
    chromeOptions.addArguments("--user-agent=\"" + task.getSite().getUserAgent() + "\"");

    Map<String, Object> prefs = new HashMap<String, Object>();
    prefs.put("profile.managed_default_content_settings.images",2); //禁止下载加载图片
    chromeOptions.setExperimentalOption("prefs", prefs);
    WebDriver driver = new ChromeDriver(chromeOptions);
    driver.get(request.getUrl());
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    String html = ((ChromeDriver) driver).findElementByXPath("//*").getAttribute("outerHTML");
    driver.close();
    driver.quit();
    Page page = new Page();
    page.setRawText(html);
    page.setUrl(new PlainText(request.getUrl()));
    page.setStatusCode(HttpStatus.SC_OK);
    page.setRequest(request);
    page.setDownloadSuccess(true);
    return page;
  }

  @Override
  public void setThread(int i) {

  }
}
