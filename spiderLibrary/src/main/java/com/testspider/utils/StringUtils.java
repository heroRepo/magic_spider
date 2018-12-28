package com.testspider.utils;

import sun.security.provider.MD5;

public class StringUtils {
    public static boolean isNullOrWhiteSpaces(String content){
        return null == content || content.trim().length() == 0;
    }

}
