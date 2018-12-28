package com.testspider.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class FileTypeUtil {
    private static Map<String, String> mimeFileTypeDic = new HashMap<String, String>(){
        {put("audio/mpeg", "mp3");}
        {put("audio/mp3", "mp3");}
        {put("video/mp4", "mp4");}
        {put("image/jpg", "jpg");}
        {put("image/jpeg", "jpg");}
        {put("image/png", "png");}
        {put("image/gif", "gif");}
        {put("image/webp", "webp");}
    } ;

    private static HashMap<String, String> supportedFileTypeDic = new HashMap<String, String>(){
        {put("mp3", "audio");}
        {put("mp4", "video");}
        {put("jpg", "image");}
        {put("png", "image");}
        {put("gif", "image");}
        {put("webp", "image");}
    };

    public static String getFileTypeDirName(String type) {
        return supportedFileTypeDic.get(type.trim().toLowerCase());
    }

    public static boolean isFileTypeSupported(String type) {
        if(type == null)
            return false;
        return supportedFileTypeDic.containsKey(type.trim().toLowerCase());
    }

    public static String getFileTypeByMime(String mime) {
        String fileType = mimeFileTypeDic.get(mime.toLowerCase());
        if(null == fileType)
            fileType = "";
        return fileType;
    }
}
