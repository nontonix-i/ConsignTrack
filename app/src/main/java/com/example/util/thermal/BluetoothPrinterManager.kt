package com.example.util.thermal

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class PairedPrinter(
    val name: String,
    val address: String
)

sealed class PrintResult {
    data object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

class BluetoothPrinterManager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothSupported(): Boolean = getBluetoothAdapter() != null

    fun isBluetoothEnabled(): Boolean = getBluetoothAdapter()?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<PairedPrinter> {
        val adapter = getBluetoothAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices?.map { device ->
                PairedPrinter(
                    name = device.name ?: "Unknown Printer",
                    address = device.address
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printData(deviceAddress: String, data: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val adapter = getBluetoothAdapter()
            ?: return@withContext PrintResult.Error("Bluetooth tidak didukung pada perangkat ini.")

        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error("Bluetooth dinonaktifkan. Silakan aktifkan Bluetooth.")
        }

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                ?: return@withContext PrintResult.Error("Perangkat printer tidak ditemukan ($deviceAddress).")

            // Cancel discovery before connecting
            try {
                adapter.cancelDiscovery()
            } catch (_: Exception) {}

            val socket = try {
                device.createRfcommSocketToServiceRecord(sppUuid)
            } catch (e: Exception) {
                // Fallback via reflection if standard fails
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                method.invoke(device, 1) as android.bluetooth.BluetoothSocket
            }

            socket.connect()
            val outputStream: OutputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            // Small sleep to ensure buffer transmission
            Thread.sleep(800)

            outputStream.close()
            socket.close()

            PrintResult.Success
        } catch (e: SecurityException) {
            PrintResult.Error("Izin Bluetooth belum diberikan: ${e.localizedMessage}")
        } catch (e: Exception) {
            PrintResult.Error("Gagal mencetak: ${e.localizedMessage ?: "Koneksi ke printer terputus."}")
        }
    }

    suspend fun printReceipt(deviceAddress: String, receipt: com.example.domain.model.ReceiptData, is80mm: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val bytes = EscPosHelper.buildEscPosBytes(receipt, is80mm)
        when (val res = printData(deviceAddress, bytes)) {
            is PrintResult.Success -> Result.success(Unit)
            is PrintResult.Error -> Result.failure(Exception(res.message))
        }
    }
}
