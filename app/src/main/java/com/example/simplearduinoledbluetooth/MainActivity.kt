package com.example.simplearduinoledbluetooth
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var retryButton: ImageView
    private lateinit var switchButton: ImageView
    private lateinit var connectionStatus: TextView

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var outputStream: OutputStream
    private var socket: BluetoothSocket? = null

    private val deviceName = "HC-05"
    private val onCommand = "1"
    private val offCommand = "0"
    private var switchOn = false

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    private val REQUEST_CODE_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        retryButton = findViewById(R.id.retryButton)
        switchButton = findViewById(R.id.switchButton)
        connectionStatus = findViewById(R.id.connectionStatus)

        if (!hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(this, bluetoothPermissions, REQUEST_CODE_PERMISSIONS)
        } else {
            initBluetoothConnection()
        }
    }

    private fun initBluetoothConnection() {
        bluetoothAdapter?.let { adapter ->
            if (!adapter.isEnabled) {
                try {
                    adapter.enable()
                    while (!adapter.isEnabled) {
                        Thread.sleep(100)
                    }
                } catch (e: SecurityException) {
                    showDisconnected("Permissão negada para ativar Bluetooth")
                    return
                }
            }

            try {
                val targetDevice = adapter.bondedDevices.find { it.name == deviceName }
                if (targetDevice != null) {
                    connect(targetDevice)
                } else {
                    showDisconnected("Dispositivo '$deviceName' não pareado")
                }
            } catch (e: SecurityException) {
                showDisconnected("Permissão negada ao acessar dispositivos pareados")
            }
        } ?: run {
            showDisconnected("Bluetooth não suportado")
        }
    }

    private fun connect(device: BluetoothDevice) {
        try {
            socket = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                .invoke(device, 1) as BluetoothSocket

            try {
                socket?.connect()
                outputStream = socket!!.outputStream
                connectionStatus.text = "Connected"
                retryButton.visibility = View.INVISIBLE
            } catch (e: SecurityException) {
                showDisconnected("Permissão negada: ${e.message}")
            } catch (e: IOException) {
                showDisconnected("Erro de conexão: ${e.message}")
            }


            connectionStatus.text = "Connected"
            retryButton.visibility = View.INVISIBLE

        } catch (e: Exception) {
            showDisconnected(e.localizedMessage ?: "Erro desconhecido")
        }
    }

    private fun dataToSend(data: String) {
        if (!::outputStream.isInitialized) {
            Toast.makeText(this, "Sem conexão com o Arduino", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            outputStream.write(data.toByteArray())
        } catch (e: IOException) {
            showDisconnected("Erro ao enviar: ${e.message}")
        }
    }


    fun sendData(view: View) {
        if (!::outputStream.isInitialized) {
            Toast.makeText(this, "Dispositivo não conectado!", Toast.LENGTH_SHORT).show()
            connectionStatus.text = "Disconnected"
            retryButton.visibility = View.VISIBLE
            return
        }

        if (switchOn) {
            dataToSend(offCommand)
            switchButton.setColorFilter(Color.argb(255, 255, 0, 0)) // vermelho
            switchOn = false
        } else {
            dataToSend(onCommand)
            switchButton.setColorFilter(Color.argb(255, 0, 255, 0)) // verde
            switchOn = true
        }
    }


    fun reConnect(view: View) {
        initBluetoothConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothAdapter?.disable()
    }

    private fun showDisconnected(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        connectionStatus.text = "Disconnected"
        retryButton.visibility = View.VISIBLE
    }

    private fun hasBluetoothPermissions(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initBluetoothConnection()
            } else {
                Toast.makeText(this, "Permissões de Bluetooth são necessárias", Toast.LENGTH_LONG).show()
            }
        }
    }
}
