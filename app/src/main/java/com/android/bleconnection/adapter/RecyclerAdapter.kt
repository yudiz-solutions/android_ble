package com.android.bleconnection.adapter

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.bleconnection.R
import com.android.bleconnection.model.SearchModel
import kotlinx.android.synthetic.main.row_scan_result.view.*


open class RecyclerAdapter() : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    private lateinit var itemsData: ArrayList<BluetoothDevice>
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.init(position, itemsData)
        holder.itemView.root?.setOnClickListener {
            onClickListener.itemClicked(itemsData[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.row_scan_result, parent, false)
        return ViewHolder(view)
    }

    private lateinit var inflater: LayoutInflater
    private lateinit var context: Context
    private lateinit var onClickListener: OnItemClick

    constructor(
        context: Context,
        itemsData: ArrayList<BluetoothDevice>,
        onClickListener: OnItemClick
    ) : this() {
        println("constructer called" + itemsData.size)
        this.inflater = LayoutInflater.from(context)
        this.itemsData = itemsData
        this.onClickListener = onClickListener
        println("market place size" + itemsData.size)
    }

    override fun getItemCount(): Int {
        return itemsData.size;
    }

    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        fun init(position: Int, itemsData: ArrayList<BluetoothDevice>) {
            if (itemsData[position].name != null)
                itemView.tv_name.text = itemsData[position]
            else
                itemView.tv_name.setText(R.string.name_not_found)
            itemView.tv_address.text = itemsData[position].address
        }
    }
}
