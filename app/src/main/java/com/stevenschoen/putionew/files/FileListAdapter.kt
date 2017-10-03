package com.stevenschoen.putionew.files

import android.support.v4.util.ArraySet
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.files.PutioFile

class FileListAdapter(private val data: List<PutioFile>,
                      val onFileClicked: (file: PutioFile, holder: FileHolder) -> Unit,
                      val onFileLongClicked: (file: PutioFile, holder: FileHolder) -> Unit)
    : RecyclerView.Adapter<FileListAdapter.FileHolder>() {

    private var itemsCheckedChangedListener: OnItemsCheckedChangedListener? = null

    val checkedIds = ArraySet<Long>()

    init {
        setHasStableIds(true)
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (isInCheckMode()) {
                    val idsToRemove = ArraySet<Long>()

                    for (checkedId in checkedIds) {
                        val stillHas = data.any { it.id == checkedId }
                        if (!stillHas) {
                            idsToRemove.add(checkedId)
                        }
                    }

                    for (id in idsToRemove) {
                        checkedIds.remove(id)
                        notifyItemChanged(getItemPosition(id))
                    }
                    if (!idsToRemove.isEmpty() && itemsCheckedChangedListener != null) {
                        itemsCheckedChangedListener!!.onItemsCheckedChanged()
                    }
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_putio, parent, false)

        return FileHolder(view)
    }

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        if (holder.itemView is Checkable) {
            holder.itemView.isChecked = isPositionChecked(position)
        }

        val file = data[position]

        holder.textName.text = file.name
        holder.textDescription.text = PutioUtils.humanReadableByteCount(file.size!!, false)
        if (file.isFolder) {
            Picasso.with(holder.iconImg.context).cancelRequest(holder.iconImg)
            holder.iconImg.setImageResource(R.drawable.ic_putio_folder_accent)
        } else {
            if (!file.icon.isNullOrEmpty()) {
                Picasso.with(holder.iconImg.context).load(file.icon).into(holder.iconImg)
            }
        }
        if (file.isAccessed) {
            holder.iconAccessed.visibility = View.VISIBLE
        } else {
            holder.iconAccessed.visibility = View.GONE
        }
    }

    override fun getItemId(position: Int): Long {
        if (position != -1 && !data.isEmpty()) {
            return data[position].id
        }

        return 0
    }

    fun getItemPosition(fileId: Long): Int {
        for (i in data.indices) {
            val file = data[i]
            if (file.id == fileId) {
                return i
            }
        }
        return -1
    }

    override fun getItemCount() = data.size

    fun isInCheckMode() = checkedIds.isNotEmpty()

    fun checkedCount() = checkedIds.size

    fun isPositionChecked(position: Int): Boolean {
        val itemId = getItemId(position)
        return (itemId != -1L) && (checkedIds.contains(itemId))
    }

    fun getCheckedPositions() = checkedIds.map { getItemPosition(it) }

    fun setPositionChecked(position: Int, checked: Boolean) {
        val itemId = getItemId(position)
        if (checked) {
            checkedIds.add(itemId)
        } else {
            checkedIds.remove(itemId)
        }
        notifyItemChanged(position)
        if (itemsCheckedChangedListener != null) {
            itemsCheckedChangedListener!!.onItemsCheckedChanged()
        }
    }

    fun togglePositionChecked(position: Int) {
        setPositionChecked(position, !isPositionChecked(position))
    }

    fun addCheckedIds(vararg ids: Long) {
        ids
                .filter { checkedIds.add(it) }
                .forEach { notifyItemChanged(getItemPosition(it)) }
        if (itemsCheckedChangedListener != null) {
            itemsCheckedChangedListener!!.onItemsCheckedChanged()
        }
    }

    fun clearChecked() {
        if (checkedIds.isNotEmpty()) {
            val previouslyCheckedIds = ArraySet<Long>(checkedIds)
            checkedIds.clear()
            for (id in previouslyCheckedIds) {
                notifyItemChanged(getItemPosition(id))
            }
            if (itemsCheckedChangedListener != null) {
                itemsCheckedChangedListener!!.onItemsCheckedChanged()
            }
        }
    }

    fun setItemsCheckedChangedListener(itemsCheckedChangedListener: OnItemsCheckedChangedListener) {
        this.itemsCheckedChangedListener = itemsCheckedChangedListener
    }

    interface OnItemsCheckedChangedListener {
        fun onItemsCheckedChanged()
    }

    inner class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.text_file_name)
        val textDescription: TextView = itemView.findViewById(R.id.text_file_description)
        val iconImg: ImageView = itemView.findViewById(R.id.icon_file_img)
        val iconAccessed: ImageView = itemView.findViewById(R.id.icon_file_accessed)

        init {
            itemView.setOnClickListener {
                onFileClicked.invoke(data[adapterPosition], this)
            }
            itemView.setOnLongClickListener {
                onFileLongClicked.invoke(data[adapterPosition], this)
                true
            }
        }
    }
}