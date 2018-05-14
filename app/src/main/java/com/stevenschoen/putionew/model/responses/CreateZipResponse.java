package com.stevenschoen.putionew.model.responses;

import com.google.gson.annotations.SerializedName;
import com.stevenschoen.putionew.model.ResponseOrError;

public class CreateZipResponse extends ResponseOrError.BasePutioResponse {
  @SerializedName("zip_id")
  private long zipId;

  public long getZipId() {
    return zipId;
  }
}
