package com.testspider.downloaders;

import com.google.common.hash.Hashing;
import com.testspider.BasePageProcessor;
import com.testspider.utils.Constants;
import com.testspider.utils.FileTypeUtil;
import com.testspider.utils.FileUtils;
import com.testspider.utils.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class AssetDownLoader {
    private static PoolingHttpClientConnectionManager connectionManager;
    private static transient Logger logger = LoggerFactory.getLogger(AssetDownLoader.class);
    private AssetDownLoader() {
        if(null == connectionManager) {
            Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory> create().
                    register("http", PlainConnectionSocketFactory.INSTANCE).
                    register("https", this.buildSSLConnectionSocketFactory()).build();

            connectionManager = new PoolingHttpClientConnectionManager(reg);
            connectionManager.setDefaultMaxPerRoute(100);
        }
    }

    private BasePageProcessor processor;
    public AssetDownLoader(BasePageProcessor processor) {
        this();
        this.processor = processor;
    }

    public String downloadAsset(String url, Site site, String referer) {
        getHttpClient(site);
        CloseableHttpClient httpClient = getHttpClient(site);
        HttpGet get = new HttpGet();
        get.setURI(URI.create(fixUrl(url, site, referer)));
        get.setHeader("Referer", referer);
        try {
           CloseableHttpResponse response = httpClient.execute(get);

           String fileType = "";
           Header contentType = response.getFirstHeader("content-type");
           if(contentType != null && contentType.getValue() != null) {
                fileType = FileTypeUtil.getFileTypeByMime(contentType.getValue().toLowerCase());
           }
           if (StringUtils.isNullOrWhiteSpaces(fileType)) {
               fileType = getFileTypeFromUrl(url);
           }
           String fileName = genFilePath(url, fileType);
           InputStream input = response.getEntity().getContent();
           File file = new File(Constants.SaveDir + processor.getSaveDirName() + "/" +  fileName);
           File dir = new File(file.getParent());
           if(!dir.exists()){
               dir.mkdirs();
           }
           if(file.exists()){
               file.delete();
           }
           FileOutputStream output = new FileOutputStream(file);
            byte[] buffer = new byte[10240];
            int l;
            while((l = input.read(buffer)) != -1) {
                output.write(buffer, 0, l);
            }
            output.flush();
            FileUtils.closeStream(input);
            FileUtils.closeStream(output);
            return fileName;
        } catch (IOException e) {
            logger.error(e.toString());
            return "";
        }
    }

    private String fixUrl(String url, Site site, String referer) {
        String lowerUrl = url.toLowerCase();
        // 如果是http 或 https的链接，可以直接请求
        if(lowerUrl.startsWith("http:") || lowerUrl.startsWith("https:")) {
            return url;
        }
        // 如果链接以//开头，则需要用当前referer的协议头填充url
        if(lowerUrl.startsWith("//")) {
            if(referer.toLowerCase().startsWith("https:")){
                url = "https:" + url;
            } else {
                url = "http:" + url;
            }
            return url;
        }
        // 其它链接情况，先判断是不是有其它协议头
        if(lowerUrl.indexOf("?") > -1) {
            lowerUrl = lowerUrl.substring(0, lowerUrl.indexOf("?"));
        }
        // 如果有协议头则返回
        if(lowerUrl.indexOf("://") > -1) {
            return lowerUrl;
        }


        return url;
    }

    private String getFileTypeFromUrl(String url) {
       int dotIndex = url.lastIndexOf(".");
       if(dotIndex > -1) {
            String fileType = url.substring(dotIndex + 1);
            if(FileTypeUtil.isFileTypeSupported(fileType)){
                return fileType.trim().toLowerCase();
            }
       }
       return "";
    }

    private String genFilePath(String url, String fileType) {
        String fileSubfix = StringUtils.isNullOrWhiteSpaces(fileType) ? "" : "." + fileType;
        String fileTypeDir = FileTypeUtil.getFileTypeDirName(fileType);
        fileTypeDir = fileTypeDir == null ? "" : fileTypeDir + "/";
        return  fileTypeDir + Hashing.md5().hashBytes(url.getBytes()).toString() + fileSubfix;
    }

    private CloseableHttpClient getHttpClient(Site site) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setConnectionManager(connectionManager);
        if (site.getUserAgent() != null) {
            httpClientBuilder.setUserAgent(site.getUserAgent());
        } else {
            httpClientBuilder.setUserAgent("");
        }
        return httpClientBuilder.build();
    }

    private SSLConnectionSocketFactory buildSSLConnectionSocketFactory() {
        try {
            return new SSLConnectionSocketFactory(this.createIgnoreVerifySSL());
        } catch (KeyManagementException var2) {
            this.logger.error("ssl connection fail", var2);
        } catch (NoSuchAlgorithmException var3) {
            this.logger.error("ssl connection fail", var3);
        }

        return SSLConnectionSocketFactory.getSocketFactory();
    }

    private SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        SSLContext sc = SSLContext.getInstance("SSLv3");
        sc.init((KeyManager[])null, new TrustManager[]{trustManager}, (SecureRandom)null);
        return sc;
    }

}
