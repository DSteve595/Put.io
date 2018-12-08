package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

import com.squareup.moshi.Json;

public class PutioMp4Status implements Parcelable {
  public static final Creator CREATOR = new Creator() {
    public PutioMp4Status createFromParcel(Parcel in) {
      return new PutioMp4Status(in);
    }

    public PutioMp4Status[] newArray(int size) {
      return new PutioMp4Status[size];
    }
  };
  public Status status;
  @Json(name = "percent_done")
  public int percentDone;
  public long size;

  public PutioMp4Status() {
  }

  public PutioMp4Status(Parcel in) {
    readFromParcel(in);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  private void readFromParcel(Parcel in) {
    status = Status.values()[in.readInt()];
    percentDone = in.readInt();
    size = in.readLong();
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(status.ordinal());
    out.writeInt(percentDone);
    out.writeLong(size);
  }

  public enum Status {
    @Json(name = "NOT_AVAILABLE")
    NotAvailable,
    @Json(name = "IN_QUEUE")
    InQueue,
    @Json(name = "PREPARING")
    Preparing,
    @Json(name = "CONVERTING")
    Converting,
    @Json(name = "FINISHING")
    Finishing,
    @Json(name = "COMPLETED")
    Completed,
    @Json(name = "ERROR")
    Error,
    AlreadyMp4, // Not from the server, just used to clarify
    NotVideo // Not from the server, just used to clarify
  }
}
