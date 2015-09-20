package org.sergez.splayer.util;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Sergii Zhuk
 */
public final class FileFormat {
  private static Set<String> supportedFormats;

  static {
    int currentApiVersion = android.os.Build.VERSION.SDK_INT;
    supportedFormats = new HashSet<String>() {
      /**
       *
       */
      private static final long serialVersionUID = -4082075857631915042L;

      {
        add("mp3");
        add("wav");
        add("midi");
        add("wma");
        add("mp4");
        add("m4a");
        add("ogg");
        add("amr");
        add("imy");
        add("ota");
        add("mid");
        add("xmf");
        add("rtttl");
        add("rtx");
      }
    };
    if (currentApiVersion >= 12) {
      supportedFormats.add("flac");
      supportedFormats.add("ts");
      supportedFormats.add("aac");
    }
  }

  /**
   * Check filename for acceptable media format /e.g. mp3, wav etc
   *
   * @param filename
   * @return
   */
  public static boolean acceptableFormat(String filename) {
    int dotPos = filename.lastIndexOf(".");
    String ext = filename.substring(dotPos + 1).toLowerCase();
    if ((dotPos > 0) && (supportedFormats.contains(ext))) {
      return true;
    } else {
      return false;
    }
  }
}
