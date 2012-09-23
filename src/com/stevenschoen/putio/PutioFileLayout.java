package com.stevenschoen.putio;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioFileLayout implements Parcelable {
	public String name;
	public String description;
	public int iconRes;

	public PutioFileLayout() {
		super();
	}

	public PutioFileLayout(String name, String description, int icon) {
		super();
		this.name = name;
		this.description = description;
		this.iconRes = icon;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioFileLayout(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		this.name = in.readString();
		this.description = in.readString();
		this.iconRes = in.readInt();
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeString(this.description);
		out.writeInt(this.iconRes);
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public PutioFileLayout createFromParcel(Parcel in) {
			return new PutioFileLayout(in);
		}

		public PutioFileLayout[] newArray(int size) {
			return new PutioFileLayout[size];
		}
	};
}