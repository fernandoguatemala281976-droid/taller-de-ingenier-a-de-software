package com.tuapp.smartwalk

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.widget.TextView
import android.widget.Button
import android.view.View
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var escaneando = false
    private var audioActivo = true

    private val SERVICE_UUID        = UUID.fromString("12345678-1234-1234-1234-123456789012")
    private val CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-4321-4321-210987654321")
    private val DESCRIPTOR_UUID     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private lateinit var tvEstado: TextView
    private lateinit var tvSensorFrente: TextView
    private lateinit var tvNivelFrente: TextView
    private lateinit var tvSensorPiso: TextView
    private lateinit var tvNivelPiso: TextView
    private lateinit var tvUltimaAlerta: TextView
    private lateinit var btnConectar: Button
    private lateinit var btnAudio: Button
    private lateinit var btnPrueba: Button
    private lateinit var indicadorFrente: View
    private lateinit var indicadorPiso: View

    private val mensajesVoz = mapOf(
        "F0" to null,
        "F1" to "Atención, objeto detectado a un metro",
        "F2" to "Precaución, objeto próximo a ochenta centímetros",
        "F3" to "Muy cerca, objeto a sesenta centímetros, reduce la velocidad",
        "F4" to "Peligro, objeto a cuarenta centímetros, detente ahora",
        "F5" to "Peligro máximo, impacto inminente, detente",
        "P0" to null,
        "P1" to "Atención, desnivel detectado en el suelo",
        "P2" to "Precaución, posible escalón frente a ti",
        "P3" to "Peligro, riesgo de caída, detente inmediatamente"
    )

    private val textoFrente = mapOf(
        "F0" to Pair("Despejado",              "> 100 cm"),
        "F1" to Pair("Aviso: objeto en rango", "81 – 100 cm"),
        "F2" to Pair("Precaución: próximo",    "61 – 80 cm"),
        "F3" to Pair("Muy cerca",              "41 – 60 cm"),
        "F4" to Pair("Peligro: detente",       "21 – 40 cm"),
        "F5" to Pair("Peligro máximo",         "0 – 20 cm")
    )

    private val textoPiso = mapOf(
        "P0" to Pair("Suelo seguro",       "Normal"),
        "P1" to Pair("Desnivel detectado", "Leve"),
        "P2" to Pair("Posible escalón",    "Moderado"),
        "P3" to Pair("Riesgo de caída",    "Grave")
    )

    private val coloresFrente = mapOf(
        "F0" to "#4CAF50",
        "F1" to "#9C27B0",
        "F2" to "#FFC107",
        "F3" to "#FF9800",
        "F4" to "#F44336",
        "F5" to "#B71C1C"
    )

    private val coloresPiso = mapOf(
        "P0" to "#4CAF50",
        "P1" to "#FFC107",
        "P2" to "#FF9800",
        "P3" to "#B71C1C"
    )

    // ── Callbacks BLE ─────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    runOnUiThread { actualizarEstadoConexion(true) }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        actualizarEstadoConexion(false)
                        reconectar()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val caracteristica = gatt
                .getService(SERVICE_UUID)
                ?.getCharacteristic(CHARACTERISTIC_UUID) ?: return

            gatt.setCharacteristicNotification(caracteristica, true)
            val descriptor = caracteristica.getDescriptor(DESCRIPTOR_UUID) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val dato = characteristic.getStringValue(0)?.trim() ?: return
            procesarDato(dato)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val dato = String(value).trim()
            procesarDato(dato)
        }
    }

    // ── Procesa dato del ESP32 ────────────────────────────────────────────
    private fun procesarDato(dato: String) {
        val mensajeVoz = mensajesVoz[dato] ?: return
        runOnUiThread {
            when {
                dato.startsWith("F") -> {
                    val info = textoFrente[dato] ?: return@runOnUiThread
                    tvSensorFrente.text = info.first
                    tvNivelFrente.text  = info.second
                    val color = coloresFrente[dato] ?: "#4CAF50"
                    indicadorFrente.setBackgroundColor(Color.parseColor(color))
                    tvUltimaAlerta.text = "Frente: ${info.first}"
                    if (audioActivo && mensajeVoz != null) {
                        tts?.speak(mensajeVoz, TextToSpeech.QUEUE_ADD, null, "frente_$dato")
                    }
                }
                dato.startsWith("P") -> {
                    val info = textoPiso[dato] ?: return@runOnUiThread
                    tvSensorPiso.text = info.first
                    tvNivelPiso.text  = info.second
                    val color = coloresPiso[dato] ?: "#4CAF50"
                    indicadorPiso.setBackgroundColor(Color.parseColor(color))
                    tvUltimaAlerta.text = "Piso: ${info.first}"
                    if (audioActivo && mensajeVoz != null) {
                        tts?.stop()
                        tts?.speak(mensajeVoz, TextToSpeech.QUEUE_FLUSH, null, "piso_$dato")
                    }
                }
            }
        }
    }

    // ── Escaneo BLE ───────────────────────────────────────────────────────
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val nombre = result.device.name ?: return
            if (nombre == "SmartWalk") {
                detenerEscaneo()
                conectar(result.device)
            }
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inicializarVistas()
        tts = TextToSpeech(this, this)
        pedirPermisos()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "MX")
            tts?.setSpeechRate(1.1f)
            tts?.setPitch(1.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
    }

    // ── Inicializar vistas ────────────────────────────────────────────────
    private fun inicializarVistas() {
        tvEstado       = findViewById(R.id.tvEstado)
        tvSensorFrente = findViewById(R.id.tvSensorFrente)
        tvNivelFrente  = findViewById(R.id.tvNivelFrente)
        tvSensorPiso   = findViewById(R.id.tvSensorPiso)
        tvNivelPiso    = findViewById(R.id.tvNivelPiso)
        tvUltimaAlerta = findViewById(R.id.tvUltimaAlerta)
        btnConectar    = findViewById(R.id.btnConectar)
        btnAudio       = findViewById(R.id.btnAudio)
        btnPrueba      = findViewById(R.id.btnPrueba)
        indicadorFrente = findViewById(R.id.indicadorFrente)
        indicadorPiso   = findViewById(R.id.indicadorPiso)

        btnConectar.setOnClickListener {
            if (!escaneando) iniciarEscaneo()
        }
        btnAudio.setOnClickListener {
            audioActivo = !audioActivo
            btnAudio.text = if (audioActivo) "🔊 Audio activo" else "🔇 Audio silenciado"
            if (!audioActivo) tts?.stop()
        }
        btnPrueba.setOnClickListener {
            tts?.speak(
                "Prueba de sistema SmartWalk. Audio funcionando correctamente.",
                TextToSpeech.QUEUE_FLUSH, null, "prueba"
            )
        }
    }

    // ── Estado conexión UI ────────────────────────────────────────────────
    private fun actualizarEstadoConexion(conectado: Boolean) {
        if (conectado) {
            tvEstado.text = "● Conectado"
            tvEstado.setTextColor(Color.parseColor("#4CAF50"))
            btnConectar.text = "✓ SmartWalk conectado"
            btnConectar.isEnabled = false
        } else {
            tvEstado.text = "● Desconectado"
            tvEstado.setTextColor(Color.parseColor("#F44336"))
            btnConectar.text = "≡ Conectar dispositivo"
            btnConectar.isEnabled = true
            tvSensorFrente.text = "---"
            tvNivelFrente.text  = "Sin datos"
            tvSensorPiso.text   = "---"
            tvNivelPiso.text    = "Sin datos"
            tvUltimaAlerta.text = "Ninguna"
            indicadorFrente.setBackgroundColor(Color.parseColor("#333333"))
            indicadorPiso.setBackgroundColor(Color.parseColor("#333333"))
        }
    }

    // ── Permisos ──────────────────────────────────────────────────────────
    private fun pedirPermisos() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            iniciarEscaneo()
        }
    }

    // ── BLE ───────────────────────────────────────────────────────────────
    private fun iniciarEscaneo() {
        val scanner = BluetoothAdapter.getDefaultAdapter()
            ?.bluetoothLeScanner ?: return
        escaneando = true
        tvEstado.text = "⟳ Buscando SmartWalk..."
        tvEstado.setTextColor(Color.parseColor("#FFC107"))
        scanner.startScan(scanCallback)
    }

    private fun detenerEscaneo() {
        val scanner = BluetoothAdapter.getDefaultAdapter()
            ?.bluetoothLeScanner ?: return
        escaneando = false
        scanner.stopScan(scanCallback)
    }

    private fun conectar(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private fun reconectar() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        Handler(Looper.getMainLooper()).postDelayed({ iniciarEscaneo() }, 3000)
    }
}