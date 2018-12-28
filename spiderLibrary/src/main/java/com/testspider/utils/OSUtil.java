package com.testspider.utils;

public class OSUtil {
  static String os;
  static boolean windows;
  static boolean mac;
  static boolean linux;
  static {
    os = System.getProperty("os.name");
    String lowerOs = os.toLowerCase();
    if (lowerOs.indexOf("windows") != -1) {
      windows = true;
    } else if (lowerOs.indexOf("linux") != -1) {
      linux = true;
    } else if (lowerOs.indexOf("mac") != -1) {
      mac = true;
    }
  }

  public static String getOs() {
    return os;
  }

  public static boolean isWindows() {
    return windows;
  }

  public static boolean isMac() {
    return mac;
  }

  public static boolean isLinux() {
    return linux;
  }

  public static String osResourcePath() {
    return windows ? "windows" : mac ? "mac" : linux ? "linux" : "";
  }
}
