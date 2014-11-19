package com.stevenschoen.putionew;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.stevenschoen.putionew.model.files.PutioFile;

import java.util.ArrayList;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileHolder> {

    private List<PutioFile> data;

    private OnItemClickListener itemClickListener;
    private OnItemsCheckedChangedListener itemsCheckedChangedListener;

    private final List<Long> checkedIds;

	public FilesAdapter(final List<PutioFile> data) {
        super();
        this.data = data;

        checkedIds = new ArrayList<>();

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (isInCheckMode()) {
                    List<Long> idsToRemove = new ArrayList<>();

                    for (long checkedId : checkedIds) {
                        boolean stillHas = false;
                        for (PutioFile file : data) {
                            if (file.id == checkedId) {
                                stillHas = true;
                                break;
                            }
                        }
                        if (!stillHas) {
                            idsToRemove.add(checkedId);
                        }
                    }

                    for (long id : idsToRemove) {
                        checkedIds.remove(id);
                        notifyItemChanged(getItemPosition(id));
                    }
                    if (!idsToRemove.isEmpty() && itemsCheckedChangedListener != null) {
                        itemsCheckedChangedListener.onItemsCheckedChanged();
                    }
                }
            }
        });
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
        if (holder.root instanceof Checkable) {
            ((Checkable) holder.root).setChecked(isPositionChecked(position));
        }

        PutioFile file = data.get(position);

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
            if (file.icon.endsWith("folder.png")) {
                Picasso.with(holder.iconImg.getContext()).load(R.drawable.ic_putio_folder).into(holder.iconImg);
            } else {
                Picasso.with(holder.iconImg.getContext()).load(file.icon).into(holder.iconImg);
            }
        }
        if (file.isAccessed()) {
            holder.iconAccessed.setVisibility(View.VISIBLE);
        } else {
            holder.iconAccessed.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position != -1 && !data.isEmpty()) {
            return data.get(position).id;
        }

        return 0;
    }

    public int getItemPosition(long fileId) {
        for (int i = 0; i < data.size(); i++) {
            PutioFile file = data.get(i);
            if (file.id == fileId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public boolean isInCheckMode() {
        return (!checkedIds.isEmpty());
    }

    public int getCheckedCount() {
        return checkedIds.size();
    }

    public boolean isPositionChecked(int position) {
        long itemId = getItemId(position);
        return (itemId != -1 && checkedIds.contains(itemId));
    }

    public int[] getCheckedPositions() {
        int[] checkedPositions = new int[checkedIds.size()];
        for (int i = 0; i < checkedIds.size(); i++) {
            long id = checkedIds.get(i);
            checkedPositions[i] = getItemPosition(id);
        }

        return checkedPositions;
    }

    public List<Long> getCheckedIds() {
        return checkedIds;
    }

    public void setPositionChecked(int position, boolean checked) {

        long itemId = getItemId(position);
        if (checked && !checkedIds.contains(itemId)) {
            checkedIds.add(itemId);
        } else if (!checked) {
            checkedIds.remove(itemId);
        }
        notifyItemChanged(position);
        if (itemsCheckedChangedListener != null) {
            itemsCheckedChangedListener.onItemsCheckedChanged();
        }
    }

    public void togglePositionChecked(int position) {
        setPositionChecked(position, !isPositionChecked(position));
    }

    public void addCheckedIds(long... ids) {
        for (long id : ids) {
            if (!checkedIds.contains(id)) {
                checkedIds.add(id);
                notifyItemChanged(getItemPosition(id));
            }
        }
        if (itemsCheckedChangedListener != null) {
            itemsCheckedChangedListener.onItemsCheckedChanged();
        }
    }

    public void clearChecked() {
        List<Long> previouslyCheckedIds = new ArrayList<>(checkedIds.size());
        previouslyCheckedIds.addAll(checkedIds);
        checkedIds.clear();
        for (Long id : previouslyCheckedIds) {
            notifyItemChanged(getItemPosition(id));
        }
        if (itemsCheckedChangedListener != null) {
            itemsCheckedChangedListener.onItemsCheckedChanged();
        }
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setItemsCheckedChangedListener(OnItemsCheckedChangedListener itemsCheckedChangedListener) {
        this.itemsCheckedChangedListener = itemsCheckedChangedListener;
    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
        public void onItemLongClick(View view, int position);
    }

    public interface OnItemsCheckedChangedListener {
        public void onItemsCheckedChanged();
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