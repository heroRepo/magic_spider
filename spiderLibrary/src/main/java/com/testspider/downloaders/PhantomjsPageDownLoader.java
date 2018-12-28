package com.testspider.downloaders;

import com.google.gson.Gson;
import com.testspider.utils.Constants;
import com.testspider.utils.PhantomjsLocator;
import com.testspider.utils.ProcessExecutor;
import lombok.Builder;
import lombok.Data;
import org.apache.http.HttpStatus;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.PlainText;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhantomjsPageDownLoader implements Downloader {
    public PhantomjsPageDownLoader() {
        PhantomjsLocator.getPhantomjsPath();
    }

    private List<String> downloadByPhantomjs(Request request) {
        String cmd = PhantomjsLocator.getPhantomjsPath();
        List<String> args = new ArrayList<>();
        args.add("--load-images=false");
        args.add(PhantomjsLocator.getAjaxRequestJsPath());
        RequestPojo requestPojo = RequestPojo.builder().url(request.getUrl()).build();
        for (Map.Entry<String, String> entry :
                request.getCookies().entrySet()) {
            requestPojo.addCookie(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry :
                request.getHeaders().entrySet()) {
            requestPojo.addHeader(entry.getKey(), entry.getValue());
        }
        try {
            String requestJson = URLEncoder.encode(new Gson().toJson(requestPojo), Constants.UTF8);
            args.add(requestJson);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ProcessExecutor executor = new ProcessExecutor();
        List<String> result = executor.execute(cmd, args.toArray(new String[]{}));
        return result;
    }

    @Override
    public Page download(Request request, Task task) {
        List<String> result = downloadByPhantomjs(request);
        StringBuilder responseContent = new StringBuilder();
        boolean hasResponse = false;
        boolean timeout = false;
        if(null != result && !result.isEmpty()) {
            for (String item : result) {
                if (item.equalsIgnoreCase("--clear--")) {
                    hasResponse = true;
                    continue;
                }
                if (item.equalsIgnoreCase("--timeout--")) {
                    timeout = true;
                    continue;
                }
                if (hasResponse) {
                    responseContent.append(item);
                }
            }
        }
        Page page = new Page();
        page.setRawText(responseContent.toString());
        page.setUrl(new PlainText(request.getUrl()));
        page.setStatusCode(HttpStatus.SC_OK);
        page.setRequest(request);
        page.setDownloadSuccess(true);
        return page;
    }

    @Override
    public void setThread(int i) {

    }

    @Data
    @Builder
    public static class RequestPojo {
        private String url;
        private Map<String, String> cookies = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();


        public RequestPojo addCookie(String name, String value) {
            if(null == cookies)
                cookies = new HashMap<>();
            cookies.put(name, value);
            return this;
        }

        public RequestPojo addHeader(String name, String value) {
            if(null == headers)
                headers = new HashMap<>();
            headers.put(name, value);
            return this;
        }
    }
}
