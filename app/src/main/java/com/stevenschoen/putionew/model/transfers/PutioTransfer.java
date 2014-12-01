package com.stevenschoen.putionew.model.transfers;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioTransfer implements Parcelable {
	public long id;
	public long fileId;
	public long size;
	public String name;
	public String estimatedTime;
	public String createdTime;
	public boolean extract;
    public float currentRatio;
	public long downSpeed;
	public long upSpeed;
	public int percentDone;
	public String status;
	public String statusMessage;
	public long saveParentId;

    @Override
	public int describeContents() {
		return 0;
	}
	
	public PutioTransfer(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.id = in.readLong();
		this.fileId = in.readLong();
		this.size = in.readLong();
		this.name = in.readString();
		this.estimatedTime = in.readString();
		this.createdTime = in.readString();
		this.extract = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
        this.currentRatio = in.readFloat();
		this.downSpeed = in.readLong();
		this.upSpeed = in.readLong();
		this.percentDone = in.readInt();
		this.status = in.readString();
		this.statusMessage = in.readString();
		this.saveParentId = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(this.id);
		out.writeLong(this.fileId);
		out.writeLong(this.size);
		out.writeString(this.name);
		out.writeString(this.estimatedTime);
		out.writeString(this.createdTime);
		out.writeValue(this.extract);
        out.writeFloat(this.currentRatio);
		out.writeLong(this.downSpeed);
		out.writeLong(this.upSpeed);
		out.writeInt(this.percentDone);
		out.writeString(this.status);
		out.writeString(this.statusMessage);
		out.writeLong(this.saveParentId);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioTransfer createFromParcel(Parcel in) {
			return new PutioTransfer(in);
		}

		public PutioTransfer[] newArray(int size) {
			return new PutioTransfer[size];
		}
	};
}