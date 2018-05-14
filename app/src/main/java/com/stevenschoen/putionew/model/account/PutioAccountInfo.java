package com.stevenschoen.putionew.model.account;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioAccountInfo implements Parcelable {
  public static final Creator CREATOR = new Creator() {
    public PutioAccountInfo createFromParcel(Parcel in) {
      return new PutioAccountInfo(in);
    }

    public PutioAccountInfo[] newArray(int size) {
      return new PutioAccountInfo[size];
    }
  };
  private String username;
  private String mail;
  private DiskInfo disk;

  public PutioAccountInfo(Parcel in) {
    readFromParcel(in);
  }

  public String getUsername() {
    return this.username;
  }

  public String getMail() {
    return this.mail;
  }

  public DiskInfo getDisk() {
    return this.disk;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  private void readFromParcel(Parcel in) {
    this.username = in.readString();
    this.mail = in.readString();
    this.disk = in.readParcelable(ClassLoader.getSystemClassLoader());
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeString(username);
    out.writeString(mail);
    out.writeParcelable(disk, 0);
  }

  public static class DiskInfo implements Parcelable {
    public static final Creator CREATOR = new Creator() {
      public DiskInfo createFromParcel(Parcel in) {
        return new DiskInfo(in);
      }

      public DiskInfo[] newArray(int size) {
        return new DiskInfo[size];
      }
    };
    private long avail;
    private long size;
    private long used;

    public DiskInfo(Parcel in) {
      readFromParcel(in);
    }

    public long getAvail() {
      return avail;
    }

    public long getSize() {
      return size;
    }

    public long getUsed() {
      return used;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    private void readFromParcel(Parcel in) {
      this.avail = in.readLong();
      this.size = in.readLong();
      this.used = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      out.writeLong(this.avail);
      out.writeLong(this.size);
      out.writeLong(this.used);
    }
  }
}
