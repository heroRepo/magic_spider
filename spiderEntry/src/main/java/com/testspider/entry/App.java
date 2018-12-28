package com.testspider.entry;

import com.testspider.utils.Constants;
import com.testspider.BasePageProcessor;
import com.testspider.processors.BuDeJiePageProcessor;
import com.testspider.processors.QiuShiBaiKeProcessor;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        // -DsaveDir=d:/data/
        String saveDir = System.getProperty("saveDir");
        if(null != saveDir) {
            Constants.SaveDir = saveDir;
        }
        BasePageProcessor.registProcesser(new BuDeJiePageProcessor());
        BasePageProcessor.registProcesser(new QiuShiBaiKeProcessor());
        BasePageProcessor.startAll();
    }
}
