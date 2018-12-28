package com.testspider.utils;

import java.io.File;
import java.io.IOException;

public class PhantomjsLocator {
    private static String phantomjsPath;
    private static String ajaxRequestJsPath;

    static  {
        String path = OSUtil.osResourcePath();

        File temp = new File(System.getProperty("java.io.tmpdir"), "spider");
        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }

        String suffix = OSUtil.isWindows() ? ".exe" : "";
        File exe = new File(temp, "phantomjs" + suffix);
        if (!exe.exists() || exe.length() == 0) {
            FileUtils.copyFile("/" + path + "/phantomjs" + suffix, exe);
        }

        if (!OSUtil.isWindows()) {
            Runtime runtime = Runtime.getRuntime();

            try {
                runtime.exec(new String[]{"/bin/chmod", "755", exe.getAbsolutePath()});
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
        PhantomjsLocator.phantomjsPath = exe.getAbsolutePath();

        File ajaxRequestJs = new File(temp, "ajaxRequest.js" );
        FileUtils.copyFile( "/ajaxRequest.js" , ajaxRequestJs);
        PhantomjsLocator.ajaxRequestJsPath = ajaxRequestJs.getAbsolutePath();
    }

    public static String getPhantomjsPath() {
        return PhantomjsLocator.phantomjsPath;
    }
    public static String getAjaxRequestJsPath() {
        return PhantomjsLocator.ajaxRequestJsPath;
    }
}
