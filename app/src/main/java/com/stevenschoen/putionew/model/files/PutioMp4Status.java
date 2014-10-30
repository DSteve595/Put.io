package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioMp4Status implements Parcelable {
    private String status;
	private long size;

    public PutioMp4Status(String status) {
        this.status = status;
    }

	public String getStatus() {
		return status;
	}

    public void setStatus(String status) {
        this.status = status;
    }

	public long getSize() {
		return size;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioMp4Status(Parcel in) {
		readFromParcel(in);
	}

	private void readFromParcel(Parcel in) {
		this.status = in.readString();
		this.size = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.status);
		out.writeLong(this.size);
	}

	public static final Creator CREATOR = new Creator() {
		public PutioMp4Status createFromParcel(Parcel in) {
			return new PutioMp4Status(in);
		}

		public PutioMp4Status[] newArray(int size) {
			return new PutioMp4Status[size];
		}
	};
}