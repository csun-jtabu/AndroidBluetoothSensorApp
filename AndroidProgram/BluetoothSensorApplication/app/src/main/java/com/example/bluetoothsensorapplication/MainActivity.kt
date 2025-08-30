package com.example.bluetoothsensorapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluetoothsensorapplication.ui.theme.BluetoothSensorApplicationTheme
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {
    // creates the app
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState) // sets up this activity/container. is the entry point to this Android app
        val reading = mutableStateOf("—") // creates a variable that can be changed dynamically for the potentiometer


        // Accelerometer Related ///////////////////////////////////
        val accelReading = mutableStateOf("X: 0, Y: 0, Z: 0") // this will get accelerometer data from device
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager // this gets the manager of all sensors
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // this gets the accelerometer specifically

        // this calls in the SensorEventListener interface which requires overriding
        // onAccuracyChanged and onSensorChanged methods
        // this will listen for any changes to the sensor and update accordingly
        val sensorListener = object : SensorEventListener
        {
            // we're not using this so nothing will be in here
            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }

            // whenever the android sensor changes, we want to update the accelReading value
            override fun onSensorChanged(p0: SensorEvent?) {
                // if there is an event, it will change the values
                if (p0 != null)
                {
                    accelReading.value = "X: ${p0.values[0]}, Y: ${p0.values[1]}, Z: ${p0.values[2]}"
                }

            }

        }
        // this basically starts listening
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        // Bluetooth Related //////////////////////////////////////////////////
        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager // this gives us access to the bluetooth service
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter // the adapter is required to connect and receive

        // this is just to check the permissions enabled on the app. it will then request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            }
        }

        // if the device doesn't support bluetooth
        if(bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
        }
        //if the device doesn't have bluetooth enabled
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Bluetooth is not enabled. Please enable it", Toast.LENGTH_LONG).show()
        }
        // we make sure the adapter isn't null and pass it into the helper function
        val helper = BluetoothHelper(bluetoothAdapter!!)

        if (bluetoothAdapter.isEnabled) {
            // this opens a thread that will connect and receive data indefinitely
            // this allows us to run other parts of the code without locking
            Thread{
                val connected = helper.connect("00:14:03:05:5A:7B") // replace with your HC-06 MAC
                if (connected) {
                    // keeps the socket connection open indefinitely
                    while (true) {
                        // receives data
                        val data = helper.receive()
                        // if there is a data received, then update the reading value
                        if (data != null) {
                            runOnUiThread {
                                reading.value = data
                            }
                        }
                    }
                }
            }.start()

        }

        // displaying everything ///////////////////////////////////////////////////////
        enableEdgeToEdge() // resizes to take up whole screen (even under the ui bar)
        setContent { // loads ui for this activity and defines the layout. this calls all Composables
            BluetoothSensorApplicationTheme{ // creates the theme for this activity
                // calls scaffold template and fits to entire screen
                // basically is the entire skeleton of app
                Scaffold(modifier = Modifier.fillMaxSize())
                {
                    // this is the entire hello world greeting object
                    innerPadding -> Greeting(name = "World", // takes the center value returned from Scaffold which is then put into the greeting function
                    modifier = Modifier.padding(innerPadding)) // draw the element, but push in from edged by inner Padding value
                    // this is the entire output from the potentiometer
                    PotentiometerOutput(reading.value, modifier = Modifier)
                    AccelerometerOutput(accelReading.value, modifier = Modifier)
                }
            }
        }
    }
}

// this creates the "hello world" text
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // surface is used to change the background color
    Surface(color = Color.Yellow)
    {
        // box is used to align the text
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text(
                text = "Hello $name!\n",
                modifier = modifier
                    .padding(5.dp)
                    .align(Alignment.TopCenter),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "I'm testing my Bluetooth\n",
                modifier = modifier
                    .padding(5.dp)
                    .align(Alignment.TopCenter)
                    .offset(0.dp, 25.dp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.Black)
            Text(
                text = "and Sensor.",
                modifier = modifier
                    .padding(5.dp)
                    .align(Alignment.TopCenter)
                    .offset(0.dp, 50.dp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.Black)
        }
    }
}

// this creates the text responsible for the potentiometer output
@Composable
fun PotentiometerOutput(reading: String, modifier: Modifier = Modifier) {
    // box is used to align the text
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        Text(text = "Potentiometer Reading:", modifier = modifier.padding(5.dp)
            .align(Alignment.Center), color = Color.Black)
        Text(text = reading, modifier = modifier.padding(5.dp)
            .align(Alignment.Center).offset(0.dp, 25.dp), color = Color.Black)
    }
}

@Composable
fun AccelerometerOutput(reading: String, modifier: Modifier = Modifier)
{
    // box is used to align the text
    Box(modifier = Modifier.fillMaxSize().offset(y = 200.dp), contentAlignment = Alignment.Center){
        Text(text = "Device Accelerometer Reading:", modifier = modifier.padding(5.dp)
            .align(Alignment.Center), color = Color.Black)
        Text(text = reading, modifier = modifier.padding(5.dp)
            .align(Alignment.Center).offset(0.dp, 25.dp), color = Color.Black)
    }
}

// just a preview of what i want displayed on the app
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BluetoothSensorApplicationTheme {
        // scaffold is the entire skeleton of the app page
        Scaffold(modifier = Modifier.fillMaxSize()) {
            innerPadding -> Box(modifier = Modifier.padding(innerPadding)) {
                Greeting("World")
                PotentiometerOutput(reading = "123")
                AccelerometerOutput(reading = "321")
            }
        }
    }
}

// this class will be used to connect to the bluetooth module, and send the potentiometer
// data back to the app
class BluetoothHelper(private val adapter: BluetoothAdapter) {

    // this is the official Bluetooth-assigned UUID for the Serial Port Profile (SPP).
    // HC06 uses only SPP for serial communication
    private val HC06_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    // this creates an object that will hold the communication channel between devices
    private var socket: BluetoothSocket? = null

    fun connect(macAddress: String): Boolean {
        // this creates an object that holds the device we are trying to connect to (HC-06 bluetooth module)
        // it can be null
        val device: BluetoothDevice? = adapter.getRemoteDevice(macAddress)
        return try {
            // this sets up the communication channel between the device and app and
            // saves it into the socket variable for reference
            socket = device?.createRfcommSocketToServiceRecord(HC06_UUID)
            try {
                // this starts the connection
                socket?.connect()
            } catch (e: IOException) {
                // if the device isn't null, let's run connectFallback
                // and pass in the non null object that we call dev
                socket = device?.let { dev -> connectFallback(dev) }
                // this starts the connection
                socket?.connect()
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // this method is in case the previous one fails bluetooth connection fails
    fun connectFallback(device: BluetoothDevice): BluetoothSocket? {
        return try {
            // this object gets the createRfcommSocket method from Java
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
            // this actually calls/runs the method. It looks for the device on Channel 1
            m.invoke(device, 1) as BluetoothSocket
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // this method will receive data from the HC06 module
    fun receive(): String? {
        return try {
            // the incoming stream of data will be stored here. 1kb is allocated for it
            val buffer = ByteArray(1024)
            // as long as the socket isn't null, and something is being received,
            // the socket will read the bytes. if it's null, then it'll be 0
            val bytesRead = socket?.inputStream?.read(buffer) ?: 0
            // the data received is then converted to a string
            String(buffer, 0, bytesRead)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}