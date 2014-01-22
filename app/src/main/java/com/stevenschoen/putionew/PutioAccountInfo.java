package com.stevenschoen.putionew;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioAccountInfo implements Parcelable {
	public String username;
	public String email;
	public long diskAvailable;
	public long diskUsed;
	public long diskSize;
	
	public PutioAccountInfo() {
		super();
	}

	public PutioAccountInfo(String username, String email,
			long diskAvailable, long diskUsed, long diskSize) {
		super();
		this.username = username;
		this.email = email;
		this.diskAvailable = diskAvailable;
		this.diskUsed = diskUsed;
		this.diskSize = diskSize;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	public PutioAccountInfo(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		this.username = in.readString();
		this.email = in.readString();
		this.diskAvailable = in.readLong();
		this.diskUsed = in.readLong();
		this.diskSize = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.username);
		out.writeString(this.email);
		out.writeLong(this.diskAvailable);
		out.writeLong(this.diskUsed);
		out.writeLong(this.diskSize);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioAccountInfo createFromParcel(Parcel in) {
			return new PutioAccountInfo(in);
		}

		public PutioAccountInfo[] newArray(int size) {
			return new PutioAccountInfo[size];
		}
	};
}