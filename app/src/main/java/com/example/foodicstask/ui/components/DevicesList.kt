package com.example.foodicstask.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodicstask.R
import com.example.foodicstask.databinding.DevicesListBinding
import com.example.foodicstask.domain.model.Device

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<Device>,
    scannedDevices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidViewBinding(
        factory = DevicesListBinding::inflate,
        modifier = modifier
    ) {
        val adapterItems = buildList {
            add(DevicesListItem.Header("Paired Devices"))
            addAll(pairedDevices.map { DevicesListItem.Item(it) })
            add(DevicesListItem.Header("Scanned Devices"))
            addAll(scannedDevices.map { DevicesListItem.Item(it) })
        }
        val adapter = DevicesListAdapter(
            onDeviceItemClick = onDeviceClick
        ).apply {
            submitList(adapterItems)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
}

class DevicesListAdapter(
    private val onDeviceItemClick: (Device) -> Unit,
) : ListAdapter<DevicesListItem, RecyclerView.ViewHolder>(DevicesDiffCallback) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DevicesListItem.Header -> VIEW_TYPE_HEADER
            is DevicesListItem.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_list_header, parent, false)
                DevicesHeaderViewHolder(view)
            }
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.devices_list_item, parent, false)
                DevicesItemViewHolder(view, onDeviceItemClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DevicesListItem.Header -> (holder as DevicesHeaderViewHolder).bind(item.title)
            is DevicesListItem.Item -> (holder as DevicesItemViewHolder).bind(item.device)
        }
    }

    class DevicesHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(title: String) {
            itemView.findViewById<TextView>(R.id.list_header).text = title
        }
    }

    class DevicesItemViewHolder(
        itemView: View,
        private val onClick: (Device) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private var currentDevice: Device? = null

        init {
            itemView.setOnClickListener {
                currentDevice?.let(onClick)
            }
        }

        fun bind(device: Device) {
            currentDevice = device
            itemView.findViewById<TextView>(R.id.device_name).text = device.name ?: "(No name)"
            itemView.findViewById<TextView>(R.id.device_address).text = device.macAddress
        }
    }
}


sealed class DevicesListItem {
    data class Header(val title: String) : DevicesListItem()
    data class Item(val device: Device) : DevicesListItem()
}

object DevicesDiffCallback : DiffUtil.ItemCallback<DevicesListItem>() {

    override fun areItemsTheSame(oldItem: DevicesListItem, newItem: DevicesListItem): Boolean {
        return when {
            oldItem is DevicesListItem.Header && newItem is DevicesListItem.Header -> oldItem.title == newItem.title
            oldItem is DevicesListItem.Item && newItem is DevicesListItem.Item -> oldItem.device.macAddress == newItem.device.macAddress
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: DevicesListItem, newItem: DevicesListItem): Boolean {
        return when {
            oldItem is DevicesListItem.Header && newItem is DevicesListItem.Header -> oldItem == newItem
            oldItem is DevicesListItem.Item && newItem is DevicesListItem.Item -> oldItem.device == newItem.device
            else -> false
        }
    }
}
