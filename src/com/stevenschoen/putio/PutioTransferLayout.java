package com.stevenschoen.putio;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioTransferLayout implements Parcelable {
	public String name;
	public long downSpeed;
	public long upSpeed;
	public int percentDone;

	public PutioTransferLayout() {
		super();
	}

	public PutioTransferLayout(String name, long downSpeed, long upSpeed, int percentDone) {
		super();
		this.name = name;
		this.downSpeed = downSpeed;
		this.upSpeed = upSpeed;
		this.percentDone = percentDone;
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
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeLong(this.downSpeed);
		out.writeLong(this.upSpeed);
		out.writeInt(this.percentDone);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public PutioTransferLayout createFromParcel(Parcel in) {
			return new PutioTransferLayout(in);
		}

		public PutioTransferLayout[] newArray(int size) {
			return new PutioTransferLayout[size];
		}
	};
}