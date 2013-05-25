package com.stevenschoen.putionew;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioNotification implements Parcelable {
	public int id;
	public String text;
	public boolean show;
	
	public PutioNotification() {
		super();
	}

	public PutioNotification(int id, String text, boolean show) {
		super();
		this.id = id;
		this.text = text;
		this.show = show;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	public PutioNotification(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		this.id = in.readInt();
		this.text = in.readString();
		this.show = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(this.id);
		out.writeString(this.text);
		out.writeValue(this.show);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public PutioNotification createFromParcel(Parcel in) {
			return new PutioNotification(in);
		}

		public PutioNotification[] newArray(int size) {
			return new PutioNotification[size];
		}
	};
}