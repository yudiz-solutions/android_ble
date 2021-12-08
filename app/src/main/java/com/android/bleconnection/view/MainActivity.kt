package com.android.bleconnection.view

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.bleconnection.R
import com.android.bleconnection.adapter.OnItemClick
import com.android.bleconnection.adapter.RecyclerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), OnItemClick {

    private lateinit var adapter: RecyclerAdapter


    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var mBluetoothGatt: BluetoothGatt

    private lateinit var dataCharacteristic: BluetoothGattCharacteristic
    private var dataDescriptor: BluetoothGattDescriptor? = null

    private val REQUEST_ENABLE_BT: Int = 101

    //Change BLE Service
    private val BLE_SERVICE = "********-****-****-****-************"

    //Change BLE Lock Characteristic
    private val BLE_LOCK_CHARACTERISTIC = "********-****-****-****-************"

    //Change BLE Battery Characteristic
    private val BLE_BATTERY_CHARACTERISTIC = "********-****-****-****-************"

    //Change BLE Battery Descriptor
    private val BLE_BATTERY_DESCRIPTOR = "********-****-****-****-************"

    private val mBluetoothGattCallBack = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i("TAG", "onConnectionStateChange $newState")
            when (newState) {
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.i("BLE_TAG", "DISCONNECTED")
                }

                BluetoothGatt.STATE_CONNECTING -> {
                    Log.i("BLE_TAG", "CONNECTING")
                }

                BluetoothGatt.STATE_CONNECTED -> {
                    Log.i("BLE_TAG", "CONNECTED")
                    mBluetoothGatt.discoverServices()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i(
                "BLE_TAG",
                "onCharacteristicRead"
            )
            updateCharacteristic(characteristic)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i("BLE_TAG", "onServicesDiscovered $status")


            if (status == BluetoothGatt.GATT_SUCCESS)
                displayBleService(gatt.services)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i("BLE_TAG", "onCharacteristicWrite ${characteristic.uuid}")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            Log.i("BLE_TAG", "onCharacteristicChanged ${characteristic.uuid}")
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            Log.i("BLE_TAG", "onDescriptorRead")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            val characteristic = descriptor.characteristic
            mBluetoothGatt.readCharacteristic(characteristic)
            Log.i("BLE_TAG", "onDescriptorWrite")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val dataCharacteristic = characteristic.value
        tv_info.text =
            "Battery Level: ${dataCharacteristic[16]} ${System.lineSeparator()}Lock Status: ${
                getLockStatus(dataCharacteristic[6].toInt())
            }"
    }

    private fun getLockStatus(status: Int): String {
        return if (status == 1) "Locked" else "Unlocked"
    }

    private val deviceList: ArrayList<BluetoothDevice> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        listener()
    }

    private fun listener() {
        iv_close.setOnClickListener {
            ble_info.visibility = View.GONE
            disConnectDevice()
        }

        button_lock.setOnClickListener {
            lockDoor()
        }

        button_unlock.setOnClickListener {
            unlockDoor()
        }
    }

    private fun init() {
        initRV()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        if (::bluetoothManager.isInitialized)
            mBluetoothAdapter = bluetoothManager.adapter

        if (isBluetoothEnabled()) {
            scanDevices()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun initRV() {
        adapter = RecyclerAdapter(this, deviceList, this)
        rv_search.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                scanDevices()
                Toast.makeText(applicationContext, "BlueTooth is now Enabled", Toast.LENGTH_SHORT)
                    .show()
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(
                    applicationContext,
                    "Error occurred while enabling",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun scanDevices() {

        mBluetoothAdapter.bluetoothLeScanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                Log.i("SCAN", "Search Result ${result?.device?.name} ${result?.device?.address}")
                updateScanResult(result?.device)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
            }
        })
    }

    private fun updateScanResult(device: BluetoothDevice?) {
        if (device!!.name != null && !alreadyExists(device.address)) {
            deviceList.add(0, device)
            rv_search.adapter?.notifyItemInserted(0)
        }
    }

    private fun alreadyExists(address: String?): Boolean {
        val data = deviceList.find {
            it.address == address
        }
        return data != null
    }

    // Check if bluetooth is enabled or not
    private fun isBluetoothEnabled(): Boolean {
        return mBluetoothAdapter.isEnabled
    }

    override fun itemClicked(device: BluetoothDevice) {
        tv_information.text = device.name
        ble_info.visibility = View.VISIBLE
        connectDevice(device)
    }

    private fun connectDevice(device: BluetoothDevice) {
        mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallBack)
    }


    fun displayBleService(gattServices: List<BluetoothGattService>) {

        val requiredService =
            gattServices.find { it.uuid == UUID.fromString(BLE_SERVICE) }

        if (requiredService != null) {
            if (requiredService.characteristics.find {
                    it.uuid == UUID.fromString(
                        BLE_LOCK_CHARACTERISTIC
                    )
                } != null) {
                enableNotification(
                    requiredService.characteristics.find {
                        it.uuid == UUID.fromString(
                            BLE_BATTERY_CHARACTERISTIC
                        )
                    }!!,
                    UUID.fromString(BLE_BATTERY_DESCRIPTOR)
                )
            }
        }
    }


    private fun disConnectDevice() {
        if (::mBluetoothGatt.isInitialized) {
            mBluetoothGatt.disconnect()
            mBluetoothGatt.close()
        }
    }


    private fun writeCharacteristics(data: ByteArray?, DLE_SERVICE: UUID, DLE_WRITE_CHAR: UUID) {
        val service = mBluetoothGatt.getService(DLE_SERVICE)

        //Check that if service is available or not
        if (service == null) {
            Log.i("BLE_TAG", "service not found!")
            return
        }

        val charc1 = service.getCharacteristic(DLE_WRITE_CHAR)

        //Check that if Characteristic is available or not
        if (charc1 == null) {
            Log.i("BLE_TAG", "Characteristic not found!")
            return
        }

        charc1.value = data
        val stat = mBluetoothGatt.writeCharacteristic(charc1)
        Log.i("BLE_TAG", "writeCharacteristic $stat")
    }

    private fun lockDoor() {
        val lockArray = ByteArray(2)
        lockArray[0] = 0
        lockArray[1] = 99

        writeCharacteristics(
            lockArray, UUID.fromString(BLE_SERVICE),
            UUID.fromString(BLE_LOCK_CHARACTERISTIC)
        )
    }

    private fun unlockDoor() {
        val lockArray = ByteArray(2)
        lockArray[0] = 0
        lockArray[1] = 111
        writeCharacteristics(
            lockArray, UUID.fromString(BLE_SERVICE),
            UUID.fromString(BLE_LOCK_CHARACTERISTIC)
        )
    }


    private fun enableNotification(
        characteristic: BluetoothGattCharacteristic,
        DESCRIPTOR_ID: UUID
    ) {
        dataCharacteristic = characteristic

        dataDescriptor = dataCharacteristic.getDescriptor(DESCRIPTOR_ID)
        mBluetoothGatt.setCharacteristicNotification(dataCharacteristic, true)
        dataDescriptor!!.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt.writeDescriptor(dataDescriptor)
    }
}