package com.testspider.processors;

import com.testspider.BasePageProcessor;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;

public class BuDeJiePageProcessor extends BasePageProcessor {
    private Site site = Site.me().setRetryTimes(3).setSleepTime(5000).
            setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
    @Override
    public Boolean isAjaxRequest(Request request) {
        // return true时，会启一个headless浏览器来加载页面，用来抓取js渲染的页面
	// 可以根据request的地址是否js渲染，来决定是否用浏览器加载。但注意浏览器加载速度慢。
        return true;
    }

    @Override
    public String getSaveDirName() {
        return "budejie";
    }

    @Override
    public String startUrl() {
        return "http://www.budejie.com/";
    }
    //http://www.budejie.com/detail-28955058.html
    @Override
    public void process(Page page) {
        page.addTargetRequests(page.getHtml().links().regex("http://www\\.budejie\\.com/detail\\-\\d+\\.html").all());
        page.putField("content", page.getHtml().xpath("//div[@class='j-r-list-c']/html()").toString().
                replaceAll("<!--.[^-]*(?=-->)-->", "").
                replaceAll("class\\=\\\".*?\\\"", "").
                replaceAll("class\\='.*?'", "").replaceAll("\\n", ""));

    }

    @Override
    public Site getSite() {
        return site;
    }
}
