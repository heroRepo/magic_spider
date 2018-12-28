package com.testspider.utils;

import java.io.File;
import java.io.IOException;

public class ChromeDriverLocator {
    private static String chromeDriverPath;

    static {
        String path = OSUtil.osResourcePath();

        File temp = new File(System.getProperty("java.io.tmpdir"), "spider");
        if (!temp.exists()) {
            temp.mkdirs();
            temp.deleteOnExit();
        }

        String suffix = OSUtil.isWindows() ? ".exe" : "";
        File exe = new File(temp, "chromedriver" + suffix);
        if (!exe.exists() || exe.length() == 0) {
            FileUtils.copyFile("/" + path + "/chromedriver" + suffix, exe);
        }

        if (!OSUtil.isWindows()) {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(new String[]{"/bin/chmod", "755", exe.getAbsolutePath()});
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
        ChromeDriverLocator.chromeDriverPath = exe.getAbsolutePath();
    }

    public static String getChromeDriverPath() {
        return ChromeDriverLocator.chromeDriverPath;
    }
}
