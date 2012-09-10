package com.stevenschoen.putio;

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
//			holder.spinner = (Spinner) row.findViewById(R.id.item_fileSpinner);
//			
//			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
//			        R.array.spinnerstuff, android.R.layout.simple_spinner_item);
//			// Specify the layout to use when the list of choices appears
//			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			// Apply the adapter to the spinner
//			holder.spinner.setAdapter(adapter);
//			
//			holder.spinner
//					.setOnItemSelectedListener(new OnItemSelectedListener() {
//
//						public void onItemSelected(AdapterView<?> parent, View view, int spnPosition, long id) {
//							Log.d("asdf", "selected spinner #" + spnPosition + " and choice " + id);
//						}
//
//						@Override
//						public void onNothingSelected(AdapterView<?> parent) {
//							// TODO Auto-generated method stub
//							
//						}
//					});

			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}

		PutioFileLayout file = data.get(position);
		holder.textName.setText(file.name);
		holder.textDescription.setText(file.description);
		holder.imgIcon.setImageResource(file.icon);

		return row;
	}
	
	static class FileHolder {
		TextView textName;
		TextView textDescription;
		ImageView imgIcon;
	}
}