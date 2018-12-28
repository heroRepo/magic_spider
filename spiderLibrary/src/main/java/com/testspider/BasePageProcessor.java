package com.testspider;

import com.testspider.utils.Constants;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;

import java.util.ArrayList;
import java.util.List;


public abstract class BasePageProcessor implements PageProcessor {
    private Spider spider;
    private static List<BasePageProcessor> processors;
    public static void registProcesser(BasePageProcessor pageProcessor) {
        if(null == processors) {
            processors = new ArrayList<>();
        }
        processors.add(pageProcessor);
    }

    public static void startAll() {
        for (BasePageProcessor processor :
                processors) {
            processor.start();
        }
    }

    public static void stopAll() {
        for (BasePageProcessor processor :
                processors) {
            processor.stop();
        }
    }

    /**
     * 当前页面是否是js渲染，是的话，需要用浏览器来请求页面
     * @param request
     * @return
     */
    public abstract Boolean isAjaxRequest(Request request);

    public abstract String getSaveDirName();

    public abstract String startUrl();

    public void start(){
        if(null == this.spider)
            this.spider = Spider.create(this).
                    addUrl(startUrl()).
                    setScheduler(new FileCacheQueueScheduler(Constants.SaveDir + "urlCache").setDuplicateRemover(new BloomFilterDuplicateRemover(1000000))).
                    addPipeline(new JsonFilePipeline(Constants.SaveDir + getSaveDirName()));
        this.spider.start();
    }

    public void stop() {
        if(null != this.spider) {
            this.spider.stop();
        }
    }
}
