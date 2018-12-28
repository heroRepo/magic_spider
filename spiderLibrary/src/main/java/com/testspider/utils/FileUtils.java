package com.testspider.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
  public static void copyFile(String path, File dest) throws RuntimeException {
    InputStream input = null;
    FileOutputStream output = null;

    try {
      input = FileUtils.class.getResourceAsStream(path);
      output = new FileOutputStream(dest);
      byte[] buffer = new byte[1024];

      int l;
      while((l = input.read(buffer)) != -1) {
        output.write(buffer, 0, l);
      }
    } catch (IOException var18) {
      throw new RuntimeException("Cannot write file " + dest.getAbsolutePath());
    } finally {
      closeStream(output);
      closeStream(input);
    }
  }

  public static void closeStream(InputStream stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (Throwable var16) {

      }
    }
  }

  public static void closeStream(OutputStream stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (Throwable var16) {

      }
    }
  }
}
