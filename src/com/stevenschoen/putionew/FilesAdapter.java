package com.stevenschoen.putionew;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class FilesAdapter extends ArrayAdapter<PutioFileLayout> {

	Context context;
	int layoutResourceId;
	List<PutioFileLayout> data = null;

	public FilesAdapter(Context context, int layoutResourceId, List<PutioFileLayout> data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FileHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			try {
				row = inflater.inflate(layoutResourceId, parent, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			holder = new FileHolder();
			holder.textName = (TextView) row.findViewById(R.id.text_fileListName);
			holder.textDescription = (TextView) row.findViewById(R.id.text_fileListDesc);
			holder.imgIcon = (ImageView) row.findViewById(R.id.img_fileIcon);

			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}

		PutioFileLayout file = data.get(position);
		holder.textName.setText(file.name);
		holder.textDescription.setText(file.description);
//		Log.d("asdf", "name is " + file.name + ", iconUrl = " + file.iconUrl);
		if (file.name.matches("Up")) {
			Log.d("asdf", "Up iconRes = " + file.iconRes);
		}
		if (file.iconUrl == null) {
			Picasso.with(context).cancelRequest(holder.imgIcon);
			holder.imgIcon.setImageResource(file.iconRes);
		} else {
			holder.imgIcon.setImageResource(0);
			Picasso.with(context).load(file.iconUrl).into(holder.imgIcon);
		}

		return row;
	}
	
	static class FileHolder {
		TextView textName;
		TextView textDescription;
		ImageView imgIcon;
	}
}