package org.sergez.splayer.util;

/**
 * @author Sergii Zhuk
 *         Date: 13.10.13
 *         Time: 14:07
 */
public class MediaFileData {
  public String duration;
  public String title;
  public String artist;

  public MediaFileData(String artist, String title, String duration) {
    this.duration = duration;
    this.title = title;
    this.artist = artist;
  }

}
