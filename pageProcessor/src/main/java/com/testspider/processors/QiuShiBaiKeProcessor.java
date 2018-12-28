package com.testspider.processors;

import com.testspider.BasePageProcessor;
import com.testspider.downloaders.AssetDownLoader;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Selectable;

public class QiuShiBaiKeProcessor extends BasePageProcessor {
    private Site site = Site.me().setRetryTimes(3).setSleepTime(5000).
            setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
    @Override
    public Boolean isAjaxRequest(Request request) {
        // return true时，会启一个headless浏览器来加载页面，用来抓取js渲染的页面
	// 可以根据request的地址是否js渲染，来决定是否用浏览器加载。但注意浏览器加载速度慢。
        return false;
    }

    @Override
    public String getSaveDirName() {
        return "qiushibaike";
    }

    @Override
    public String startUrl() {
        return "https://www.qiushibaike.com/";
    }
    //https://www.qiushibaike.com/article/121297345
    @Override
    public void process(Page page) {
        // 解析当前网页内的文章地址，添入待选请求地址集
        page.addTargetRequests(page.getHtml().links().regex("https://www\\.qiushibaike\\.com/article/\\d+").all());
        if(page.getRequest().getUrl().indexOf("article") > -1) {
            StringBuilder content = new StringBuilder();
            AssetDownLoader downLoader = new AssetDownLoader(this);

            // 获取主文字和图片内容
            Selectable textAndImage = page.getHtml().xpath("//div[@id=\"single-next-link\"]/html()");
            String textAndImageHtml = textAndImage.toString();
            Selectable images = page.getHtml().xpath("//div[@id=\"single-next-link\"]/div[@class=\"thumb\"]/img/@src");
            // 下载文字配图
            if(images != null && images.get() != null) {
                for (String imageUrl :
                        images.all()) {
                    String localImageFile = downLoader.downloadAsset(imageUrl, getSite(), page.getUrl().get());
                    textAndImageHtml = textAndImageHtml.replace(imageUrl, localImageFile);
                }
            }
            content.append(textAndImageHtml);

            Selectable video = page.getHtml().xpath("//video[@id=\"article-video\"]/outerHtml()");
            // 获取视频内容
            if(video != null && video.get() != null) {
                String postUrl = page.getHtml().xpath("//video[@id=\"article-video\"]/@poster").toString();
                String videoUrl = page.getHtml().xpath("//video[@id=\"article-video\"]/source/@src").toString();
                String localImageFile = downLoader.downloadAsset(postUrl, getSite(), page.getUrl().get());
                String localVideoFile = downLoader.downloadAsset(videoUrl, getSite(), page.getUrl().get());
                content.append(video.toString().replace(postUrl, localImageFile).replace(videoUrl, localVideoFile));
            }
            page.putField("content", content.toString().
                    replaceAll("<!--.[^-]*(?=-->)-->", "").
                    replaceAll("class\\=\\\".*?\\\"", "").
                    replaceAll("class\\='.*?'", "").replaceAll("\\n", ""));
        } else {
            page.setSkip(true);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
