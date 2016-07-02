package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class PutioMp4Status implements Parcelable {
	public Status status;
	@SerializedName("percent_done")
	public int percentDone;
	public long size;

	public PutioMp4Status() { }

	public enum Status {
		@SerializedName("NOT_AVAILABLE")
		NotAvailable,
		@SerializedName("IN_QUEUE")
		InQueue,
		@SerializedName("PREPARING")
		Preparing,
		@SerializedName("CONVERTING")
		Converting,
		@SerializedName("FINISHING")
		Finishing,
		@SerializedName("COMPLETED")
		Completed,
		@SerializedName("ERROR")
		Error,
		AlreadyMp4 // Not from the server, just used to clarify
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioMp4Status(Parcel in) {
		readFromParcel(in);
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

	public static final Creator CREATOR = new Creator() {
		public PutioMp4Status createFromParcel(Parcel in) {
			return new PutioMp4Status(in);
		}

		public PutioMp4Status[] newArray(int size) {
			return new PutioMp4Status[size];
		}
	};
}