package com.stevenschoen.putionew;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioTransferLayout implements Parcelable {
	public String name;
	public long downSpeed;
	public long upSpeed;
	public int percentDone;
	public String status;

	public PutioTransferLayout() {
		super();
	}

	public PutioTransferLayout(String name, long downSpeed, long upSpeed, int percentDone, String status) {
		super();
		this.name = name;
		this.downSpeed = downSpeed;
		this.upSpeed = upSpeed;
		this.percentDone = percentDone;
		this.status = status;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioTransferLayout(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		this.name = in.readString();
		this.downSpeed = in.readLong();
		this.upSpeed = in.readLong();
		this.percentDone = in.readInt();
		this.status = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeLong(this.downSpeed);
		out.writeLong(this.upSpeed);
		out.writeInt(this.percentDone);
		out.writeString(this.status);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioTransferLayout createFromParcel(Parcel in) {
			return new PutioTransferLayout(in);
		}

		public PutioTransferLayout[] newArray(int size) {
			return new PutioTransferLayout[size];
		}
	};
}