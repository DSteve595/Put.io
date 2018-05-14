package com.stevenschoen.putionew.model.files;

import com.stevenschoen.putionew.PutioUtils;

public class PutioSubtitle {
  public static final String FORMAT_SRT = "srt";
  public static final String FORMAT_WEBVTT = "webvtt";

  private String key;
  private String language;
  private String name;
  private String source;

  public String getKey() {
    return key;
  }

  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public String getSource() {
    return source;
  }

  public String getUrl(String format, long fileId, String tokenWithStuff) {
    return PutioUtils.baseUrl + "files/" + fileId + "/subtitles/" + getKey() + tokenWithStuff
        + "&format=" + format;
  }
}
