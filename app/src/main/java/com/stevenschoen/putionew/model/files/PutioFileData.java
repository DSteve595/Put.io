package com.stevenschoen.putionew.model.files;

import android.os.Parcel;
import android.os.Parcelable;

import com.stevenschoen.putionew.R;

import java.util.HashMap;
import java.util.Map;

public class PutioFileData implements Parcelable {
	public boolean isShared;
	public String name;
	public String icon;
	public String screenshot;
	public String createdAt;
    public String firstAccessedAt;
	public int parentId;
	public boolean isMp4Available;
	public String contentType;
	public int id;
	public long size;

    public PutioFileData() { }

	public boolean isFolder() {
		return contentType.equals("application/x-directory");
	}

	public static Map<String, Integer> contentTypes = new HashMap<>();
	static {
		contentTypes.put("file", R.drawable.ic_putio_file);
		contentTypes.put("application/x-directory", R.drawable.ic_putio_folder);
		contentTypes.put("application/x-iso9660-image", R.drawable.ic_putio_image);
		contentTypes.put("application/zip", R.drawable.ic_putio_compressed);
		contentTypes.put("application/x-rar", R.drawable.ic_putio_compressed);
		contentTypes.put("application/x-dosexec", R.drawable.ic_putio_file);
		contentTypes.put("application/pdf", R.drawable.ic_putio_pdf);
		contentTypes.put("text/plain", R.drawable.ic_putio_text);
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	public PutioFileData(Parcel in) {
		readFromParcel(in);
	}
	
	private void readFromParcel(Parcel in) {
		this.isShared = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.name = in.readString();
		this.icon = in.readString();
		this.screenshot = in.readString();
		this.createdAt = in.readString();
        this.firstAccessedAt = in.readString();
		this.parentId = in.readInt();
		this.isMp4Available = (Boolean) in.readValue(ClassLoader.getSystemClassLoader());
		this.contentType = in.readString();
		this.id = in.readInt();
		this.size = in.readLong();
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeValue(this.isShared);
		out.writeString(this.name);
		out.writeString(this.icon);
		out.writeString(this.screenshot);
		out.writeString(this.createdAt);
        out.writeString(this.firstAccessedAt);
		out.writeInt(this.parentId);
		out.writeValue(this.isMp4Available);
		out.writeString(this.contentType);
		out.writeInt(this.id);
		out.writeLong(this.size);
	}
	
	public static final Creator CREATOR = new Creator() {
		public PutioFileData createFromParcel(Parcel in) {
			return new PutioFileData(in);
		}

		public PutioFileData[] newArray(int size) {
			return new PutioFileData[size];
		}
	};
}