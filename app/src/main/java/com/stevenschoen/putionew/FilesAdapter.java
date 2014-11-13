package com.stevenschoen.putionew;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.model.files.PutioFileData;

import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileHolder> {

    private List<PutioFileData> data;

    private OnItemClickListener itemClickListener;

	public FilesAdapter(List<PutioFileData> data) {
        super();
        this.data = data;
	}

    @Override
    public FileHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.file_putio, viewGroup, false);

        FileHolder holder = new FileHolder(view);
        holder.root = view;
        holder.textName = (TextView) view.findViewById(R.id.text_file_name);
        holder.textDescription = (TextView) view.findViewById(R.id.text_file_description);
        holder.iconImg = (ImageView) view.findViewById(R.id.icon_file_img);
        holder.iconAccessed = (ImageView) view.findViewById(R.id.icon_file_accessed);

        return holder;
    }

    @Override
    public void onBindViewHolder(final FileHolder holder, final int position) {
        PutioFileData file = data.get(position);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(holder.root, position);
                }
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemLongClick(holder.root, position);
                    return true;
                }
                return false;
            }
        });
        holder.textName.setText(file.name);
        holder.textDescription.setText(PutioUtils.humanReadableByteCount(file.size, false));
        if (file.icon != null && !file.icon.isEmpty()) {
            Picasso.with(holder.iconImg.getContext()).load(file.icon).into(holder.iconImg);
        }
        if (file.isAccessed()) {
            holder.iconAccessed.setVisibility(View.VISIBLE);
        } else {
            holder.iconAccessed.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position != ListView.INVALID_POSITION && !data.isEmpty()) {
            return data.get(position).id;
        }

        return 0;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
        public void onItemLongClick(View view, int position);
    }

    public static class FileHolder extends RecyclerView.ViewHolder {
        public View root;
		public TextView textName;
		public TextView textDescription;
		public ImageView iconImg;
        public ImageView iconAccessed;

        public FileHolder(View itemView) {
            super(itemView);
        }
    }
}