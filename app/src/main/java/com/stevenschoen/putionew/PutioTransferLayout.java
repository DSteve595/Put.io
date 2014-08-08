package com.stevenschoen.putionew;

import android.os.Parcel;
import android.os.Parcelable;

import com.stevenschoen.putionew.model.transfers.PutioTransferData;

public class PutioTransferLayout implements Parcelable {
	public String name;
	public long downSpeed;
	public long upSpeed;
    public float ratio;
	public int percentDone;
	public String status;

    public PutioTransferLayout(PutioTransferData data) {
        name = data.name;
        downSpeed = data.downSpeed;
        upSpeed = data.upSpeed;
        ratio = data.currentRatio;
        percentDone = data.percentDone;
        status = data.status;
    }

	@Override
	public int describeContents() {
		return 0;
	}

	public PutioTransferLayout(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.name = in.readString();
		this.downSpeed = in.readLong();
		this.upSpeed = in.readLong();
        this.ratio = in.readFloat();
		this.percentDone = in.readInt();
		this.status = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(this.name);
		out.writeLong(this.downSpeed);
		out.writeLong(this.upSpeed);
        out.writeFloat(this.ratio);
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