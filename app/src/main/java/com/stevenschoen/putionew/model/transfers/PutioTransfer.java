package com.stevenschoen.putionew.model.transfers;

import android.os.Parcel;
import android.os.Parcelable;

public class PutioTransfer implements Parcelable {
	public long id;
	public long fileId;
	public long size;
	public String name;
	public long estimatedTime;
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
	public boolean equals(Object o) {
		if (o instanceof PutioTransfer) {
			PutioTransfer transfer = (PutioTransfer) o;
			return (transfer.id == id && transfer.fileId == fileId && transfer.size == size
					&& equalsNullSafe(transfer.name, name) && transfer.estimatedTime == estimatedTime
					&& equalsNullSafe(transfer.createdTime, createdTime) && transfer.extract == extract
					&& transfer.currentRatio == currentRatio && transfer.downSpeed == downSpeed
					&& transfer.upSpeed == upSpeed && transfer.percentDone == percentDone
					&& equalsNullSafe(transfer.status, status) && equalsNullSafe(transfer.status, status)
					&& transfer.saveParentId == saveParentId);
		}
		return super.equals(o);
	}

	private static boolean equalsNullSafe(Object o1, Object o2) {
		if (o1 == null) {
			return (o2 == null);
		} else
			return (o2 != null && o1.equals(o2));
	}

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
		this.estimatedTime = in.readLong();
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
		out.writeLong(this.estimatedTime);
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