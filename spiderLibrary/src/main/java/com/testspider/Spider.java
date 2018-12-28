package com.testspider;


import com.testspider.downloaders.ChromePageDownLoader;
import com.testspider.downloaders.PhantomjsPageDownLoader;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.*;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.pipeline.CollectorPipeline;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.pipeline.ResultItemsCollectorPipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.scheduler.Scheduler;
import us.codecraft.webmagic.thread.CountableThreadPool;
import us.codecraft.webmagic.utils.UrlUtils;
import us.codecraft.webmagic.utils.WMCollections;

public class Spider implements Runnable, Task {
    protected Downloader downloader;
    protected Downloader ajaxDownLoader;
    protected List<Pipeline> pipelines = new ArrayList();
    protected PageProcessor pageProcessor;
    protected List<Request> startRequests;
    protected Site site;
    protected String uuid;
    protected Scheduler scheduler = new QueueScheduler();
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected CountableThreadPool threadPool;
    protected ExecutorService executorService;
    protected int threadNum = 1;
    protected AtomicInteger stat = new AtomicInteger(0);
    protected boolean exitWhenComplete = true;
    protected static final int STAT_INIT = 0;
    protected static final int STAT_RUNNING = 1;
    protected static final int STAT_STOPPED = 2;
    protected boolean spawnUrl = true;
    protected boolean destroyWhenExit = true;
    private ReentrantLock newUrlLock = new ReentrantLock();
    private Condition newUrlCondition;
    private List<SpiderListener> spiderListeners;
    private final AtomicLong pageCount;
    private Date startTime;
    private int emptySleepTime;

    public static Spider create(PageProcessor pageProcessor) {
        return new Spider(pageProcessor);
    }

    public Spider(PageProcessor pageProcessor) {
        this.newUrlCondition = this.newUrlLock.newCondition();
        this.pageCount = new AtomicLong(0L);
        this.emptySleepTime = 30000;
        this.pageProcessor = pageProcessor;
        this.site = pageProcessor.getSite();
    }

    public Spider startUrls(List<String> startUrls) {
        this.checkIfRunning();
        this.startRequests = UrlUtils.convertToRequests(startUrls);
        return this;
    }

    public Spider startRequest(List<Request> startRequests) {
        this.checkIfRunning();
        this.startRequests = startRequests;
        return this;
    }

    public Spider setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    /** @deprecated */
    @Deprecated
    public Spider scheduler(Scheduler scheduler) {
        return this.setScheduler(scheduler);
    }

    public Spider setScheduler(Scheduler scheduler) {
        this.checkIfRunning();
        Scheduler oldScheduler = this.scheduler;
        this.scheduler = scheduler;
        Request request;
        if (oldScheduler != null) {
            while((request = oldScheduler.poll(this)) != null) {
                this.scheduler.push(request, this);
            }
        }

        return this;
    }

    /** @deprecated */
    public Spider pipeline(Pipeline pipeline) {
        return this.addPipeline(pipeline);
    }

    public Spider addPipeline(Pipeline pipeline) {
        this.checkIfRunning();
        this.pipelines.add(pipeline);
        return this;
    }

    public Spider setPipelines(List<Pipeline> pipelines) {
        this.checkIfRunning();
        this.pipelines = pipelines;
        return this;
    }

    public Spider clearPipeline() {
        this.pipelines = new ArrayList();
        return this;
    }

    /** @deprecated */
    public Spider downloader(Downloader downloader) {
        return this.setDownloader(downloader);
    }

    public Spider setDownloader(Downloader downloader) {
        this.checkIfRunning();
        this.downloader = downloader;
        return this;
    }

    protected void initComponent() {
        if (this.downloader == null) {
            this.downloader = new HttpClientDownloader();
        }

        if (this.pipelines.isEmpty()) {
            this.pipelines.add(new ConsolePipeline());
        }

        this.downloader.setThread(this.threadNum);
        if (this.threadPool == null || this.threadPool.isShutdown()) {
            if (this.executorService != null && !this.executorService.isShutdown()) {
                this.threadPool = new CountableThreadPool(this.threadNum, this.executorService);
            } else {
                this.threadPool = new CountableThreadPool(this.threadNum);
            }
        }

        if (this.startRequests != null) {
            Iterator var1 = this.startRequests.iterator();

            while(var1.hasNext()) {
                Request request = (Request)var1.next();
                this.addRequest(request);
            }

            this.startRequests.clear();
        }

        this.startTime = new Date();
    }

    public void run() {
        this.checkRunningStat();
        this.initComponent();
        this.logger.info("Spider {} started!", this.getUUID());

        while(!Thread.currentThread().isInterrupted() && this.stat.get() == 1) {
            final Request request = this.scheduler.poll(this);
            if (request == null) {
                if (this.threadPool.getThreadAlive() == 0 && this.exitWhenComplete) {
                    break;
                }

                this.waitNewUrl();
            } else {
                this.threadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Spider.this.processRequest(request);
                            Spider.this.onSuccess(request);
                        } catch (Exception var5) {
                            Spider.this.onError(request);
                            Spider.this.logger.error("process request " + request + " error", var5);
                        } finally {
                            Spider.this.pageCount.incrementAndGet();
                            Spider.this.signalNewUrl();
                        }

                    }
                });
            }
        }

        this.stat.set(2);
        if (this.destroyWhenExit) {
            this.close();
        }

        this.logger.info("Spider {} closed! {} pages downloaded.", this.getUUID(), this.pageCount.get());
    }

    protected void onError(Request request) {
        if (CollectionUtils.isNotEmpty(this.spiderListeners)) {
            Iterator var2 = this.spiderListeners.iterator();

            while(var2.hasNext()) {
                SpiderListener spiderListener = (SpiderListener)var2.next();
                spiderListener.onError(request);
            }
        }

    }

    protected void onSuccess(Request request) {
        if (CollectionUtils.isNotEmpty(this.spiderListeners)) {
            Iterator var2 = this.spiderListeners.iterator();

            while(var2.hasNext()) {
                SpiderListener spiderListener = (SpiderListener)var2.next();
                spiderListener.onSuccess(request);
            }
        }

    }

    private void checkRunningStat() {
        int statNow;
        do {
            statNow = this.stat.get();
            if (statNow == 1) {
                throw new IllegalStateException("Spider is already running!");
            }
        } while(!this.stat.compareAndSet(statNow, 1));

    }

    public void close() {
        this.destroyEach(this.downloader);
        this.destroyEach(this.pageProcessor);
        this.destroyEach(this.scheduler);
        Iterator var1 = this.pipelines.iterator();

        while(var1.hasNext()) {
            Pipeline pipeline = (Pipeline)var1.next();
            this.destroyEach(pipeline);
        }

        this.threadPool.shutdown();
    }

    private void destroyEach(Object object) {
        if (object instanceof Closeable) {
            try {
                ((Closeable)object).close();
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }

    }

    public void test(String... urls) {
        this.initComponent();
        if (urls.length > 0) {
            String[] var2 = urls;
            int var3 = urls.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                String url = var2[var4];
                this.processRequest(new Request(url));
            }
        }
    }

    private void processRequest(Request request) {
        boolean ajax = false;
        if(this.pageProcessor instanceof BasePageProcessor) {
            ajax = ((BasePageProcessor)this.pageProcessor).isAjaxRequest(request);
        }
        Downloader downloader = this.downloader;
        if(ajax) {
            if(null == ajaxDownLoader) {
                ajaxDownLoader = new ChromePageDownLoader();
            }
            downloader = this.ajaxDownLoader;
        }
        Page page = downloader.download(request, this);
        if (page.isDownloadSuccess()) {
            this.onDownloadSuccess(request, page);
        } else {
            this.onDownloaderFail(request);
        }
    }

    private void onDownloadSuccess(Request request, Page page) {
        if (this.site.getAcceptStatCode().contains(page.getStatusCode())) {
            this.pageProcessor.process(page);
            this.extractAndAddRequests(page, this.spawnUrl);
            if (!page.getResultItems().isSkip()) {
                Iterator var3 = this.pipelines.iterator();

                while(var3.hasNext()) {
                    Pipeline pipeline = (Pipeline)var3.next();
                    pipeline.process(page.getResultItems(), this);
                }
            }
        } else {
            this.logger.info("page status code error, page {} , code: {}", request.getUrl(), page.getStatusCode());
        }

        this.sleep(this.site.getSleepTime());
    }

    private void onDownloaderFail(Request request) {
        if (this.site.getCycleRetryTimes() == 0) {
            this.sleep(this.site.getSleepTime());
        } else {
            this.doCycleRetry(request);
        }

    }

    private void doCycleRetry(Request request) {
        Object cycleTriedTimesObject = request.getExtra("_cycle_tried_times");
        if (cycleTriedTimesObject == null) {
            this.addRequest(((Request)SerializationUtils.clone(request)).setPriority(0L).putExtra("_cycle_tried_times", 1));
        } else {
            int cycleTriedTimes = (Integer)cycleTriedTimesObject;
            ++cycleTriedTimes;
            if (cycleTriedTimes < this.site.getCycleRetryTimes()) {
                this.addRequest(((Request)SerializationUtils.clone(request)).setPriority(0L).putExtra("_cycle_tried_times", cycleTriedTimes));
            }
        }

        this.sleep(this.site.getRetrySleepTime());
    }

    protected void sleep(int time) {
        try {
            Thread.sleep((long)time);
        } catch (InterruptedException var3) {
            this.logger.error("Thread interrupted when sleep", var3);
        }

    }

    protected void extractAndAddRequests(Page page, boolean spawnUrl) {
        if (spawnUrl && CollectionUtils.isNotEmpty(page.getTargetRequests())) {
            Iterator var3 = page.getTargetRequests().iterator();

            while(var3.hasNext()) {
                Request request = (Request)var3.next();
                this.addRequest(request);
            }
        }

    }

    private void addRequest(Request request) {
        if (this.site.getDomain() == null && request != null && request.getUrl() != null) {
            this.site.setDomain(UrlUtils.getDomain(request.getUrl()));
        }

        this.scheduler.push(request, this);
    }

    protected void checkIfRunning() {
        if (this.stat.get() == 1) {
            throw new IllegalStateException("Spider is already running!");
        }
    }

    public void runAsync() {
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }

    public Spider addUrl(String... urls) {
        String[] var2 = urls;
        int var3 = urls.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String url = var2[var4];
            this.addRequest(new Request(url));
        }

        this.signalNewUrl();
        return this;
    }

    public <T> List<T> getAll(Collection<String> urls) {
        this.destroyWhenExit = false;
        this.spawnUrl = false;
        if (this.startRequests != null) {
            this.startRequests.clear();
        }

        Iterator var2 = UrlUtils.convertToRequests(urls).iterator();

        while(var2.hasNext()) {
            Request request = (Request)var2.next();
            this.addRequest(request);
        }

        CollectorPipeline collectorPipeline = this.getCollectorPipeline();
        this.pipelines.add(collectorPipeline);
        this.run();
        this.spawnUrl = true;
        this.destroyWhenExit = true;
        return collectorPipeline.getCollected();
    }

    protected CollectorPipeline getCollectorPipeline() {
        return new ResultItemsCollectorPipeline();
    }

    public <T> T get(String url) {
        List<String> urls = WMCollections.newArrayList(new String[]{url});
        List<T> resultItemses = this.getAll(urls);
        return resultItemses != null && resultItemses.size() > 0 ? resultItemses.get(0) : null;
    }

    public Spider addRequest(Request... requests) {
        Request[] var2 = requests;
        int var3 = requests.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Request request = var2[var4];
            this.addRequest(request);
        }

        this.signalNewUrl();
        return this;
    }

    private void waitNewUrl() {
        this.newUrlLock.lock();

        try {
            if (this.threadPool.getThreadAlive() == 0 && this.exitWhenComplete) {
                return;
            }

            this.newUrlCondition.await((long)this.emptySleepTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException var5) {
            this.logger.warn("waitNewUrl - interrupted, error {}", var5);
        } finally {
            this.newUrlLock.unlock();
        }

    }

    private void signalNewUrl() {
        try {
            this.newUrlLock.lock();
            this.newUrlCondition.signalAll();
        } finally {
            this.newUrlLock.unlock();
        }

    }

    public void start() {
        this.runAsync();
    }

    public void stop() {
        if (this.stat.compareAndSet(1, 2)) {
            this.logger.info("Spider " + this.getUUID() + " stop success!");
        } else {
            this.logger.info("Spider " + this.getUUID() + " stop fail!");
        }

    }

    public Spider thread(int threadNum) {
        this.checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        } else {
            return this;
        }
    }

    public Spider thread(ExecutorService executorService, int threadNum) {
        this.checkIfRunning();
        this.threadNum = threadNum;
        if (threadNum <= 0) {
            throw new IllegalArgumentException("threadNum should be more than one!");
        } else {
            this.executorService = executorService;
            return this;
        }
    }

    public boolean isExitWhenComplete() {
        return this.exitWhenComplete;
    }

    public Spider setExitWhenComplete(boolean exitWhenComplete) {
        this.exitWhenComplete = exitWhenComplete;
        return this;
    }

    public boolean isSpawnUrl() {
        return this.spawnUrl;
    }

    public long getPageCount() {
        return this.pageCount.get();
    }

    public us.codecraft.webmagic.Spider.Status getStatus() {
        return us.codecraft.webmagic.Spider.Status.fromValue(this.stat.get());
    }

    public int getThreadAlive() {
        return this.threadPool == null ? 0 : this.threadPool.getThreadAlive();
    }

    public Spider setSpawnUrl(boolean spawnUrl) {
        this.spawnUrl = spawnUrl;
        return this;
    }

    public String getUUID() {
        if (this.uuid != null) {
            return this.uuid;
        } else if (this.site != null) {
            return this.site.getDomain();
        } else {
            this.uuid = UUID.randomUUID().toString();
            return this.uuid;
        }
    }

    public Spider setExecutorService(ExecutorService executorService) {
        this.checkIfRunning();
        this.executorService = executorService;
        return this;
    }

    public Site getSite() {
        return this.site;
    }

    public List<SpiderListener> getSpiderListeners() {
        return this.spiderListeners;
    }

    public Spider setSpiderListeners(List<SpiderListener> spiderListeners) {
        this.spiderListeners = spiderListeners;
        return this;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public void setEmptySleepTime(int emptySleepTime) {
        this.emptySleepTime = emptySleepTime;
    }
}
