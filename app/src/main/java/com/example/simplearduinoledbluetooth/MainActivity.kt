package com.example.simplearduinoledbluetooth


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var onButton: Button
    private lateinit var offButton: Button
    private lateinit var statusTextView: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        setupListeners()
    }

    private fun initializeUI() {
        connectButton = findViewById(R.id.button)
        onButton = findViewById(R.id.button2)
        offButton = findViewById(R.id.button3)
        statusTextView = findViewById(R.id.textView2)
    }

    private fun setupListeners() {
        connectButton.setOnClickListener { connectToDevice() }
        onButton.setOnClickListener { sendSignal(1) }
        offButton.setOnClickListener { sendSignal(0) }
    }

    private fun connectToDevice() {
        if (isConnected) {
            showToast("Já conectado. Reinicie o app para reconectar.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 1)
            return
        }

        val pairedDevices = bluetoothAdapter?.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            showToast("Nenhum dispositivo pareado encontrado.")
            return
        }

        val device = pairedDevices.first()
        statusTextView.text = "Conectando a: ${device.name}"

        ConnectThread(device).start()
    }


    private fun sendSignal(value: Int) {
        if (!isConnected) {
            showToast("Conecte ao Arduino primeiro.")
            return
        }

        try {
            outputStream?.write(value)
            Log.d(TAG, "Sinal $value enviado com sucesso.")
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao enviar sinal", e)
            showToast("Erro ao enviar sinal: ${e.message}")
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val uuid: UUID = UUID.fromString(SERIAL_UUID)

        override fun run() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                ) {
                    runOnUiThread {
                        showToast("Permissão Bluetooth necessária para continuar")
                    }
                    return
                }

                bluetoothAdapter?.cancelDiscovery()

                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                isConnected = true

                runOnUiThread {
                    showToast("Conexão estabelecida com ${device.name}")
                    statusTextView.text = "Conectado a: ${device.name}"
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "Permissão não concedida", e)
                runOnUiThread {
                    showToast("Permissão não concedida")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Erro ao conectar", e)
                closeConnection()
                runOnUiThread {
                    showToast("Erro na conexão: ${e.message}")
                }
            }
        }


    }

    private fun closeConnection() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Erro ao fechar socket", e)
        }
        isConnected = false
        outputStream = null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToDevice()
        } else {
            showToast("Permissão Bluetooth negada.")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val SERIAL_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}