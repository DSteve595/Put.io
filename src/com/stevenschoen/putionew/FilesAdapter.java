package com.stevenschoen.putionew;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
		holder.imgIcon.setImageResource(file.iconRes);

		return row;
	}
	
	static class FileHolder {
		TextView textName;
		TextView textDescription;
		ImageView imgIcon;
	}
}