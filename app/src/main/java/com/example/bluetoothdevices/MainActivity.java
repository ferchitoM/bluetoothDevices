package com.example.bluetoothdevices;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Creamos las variables necesarias para manipular el Bluetooth del teléfono
    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<String> devicesList; //El arreglo para almacenar los dispositivos encontrados
    private static final int PERMISSION_OK = 1; //El valor que asignaremos a los permisos (1: Activado)


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Monitoreamos constantemente el proceso de búsqueda de dispositivos con el recibidor
    //TODO: Este método se activa automáticamente cuando pulsemos el botón de buscar
    //TODO: Tal vez esta función sea de las últimas que se van a ejecutar en el flujo de la aplicación

    @SuppressLint("MissingPermission") //Desactivamos las sugerencias de código innecesarias
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        //Cuando onReceive nos envíe algún evento lo manejamos
        public void onReceive(Context context, Intent intent) {

            //Si el intento arroja ACTION_FOUND significa que se ha encontrado un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice.class); //Capturamos el dispositivo encontrado

                assert device != null; //Comprobamos que el dispositivo no sea nulo

                //Si el nombre del dispositivo es nulo, lo cambiamos por "Dispositivo desconocido"
                String deviceName = device.getName() != null
                        ? device.getName()
                        : "Dispositivo desconocido";

                //Extraemos la dirección MAC y otros valores que deseemos
                String deviceInfo =
                        "NOMBRE: " + deviceName + "\nMAC: " + device.getAddress() + "\nCLASE: " + device.getBluetoothClass() + "\n";

                //Lanzamos un log para ver en consola los dispositivos encontrados
                Log.e("Dispositivos encontrados: ", deviceInfo);

                //Validamos si que el dispositivo encontrado NO se repite o NO está en el arreglo devicesList
                if (!devicesList.contains(deviceInfo)) {
                    devicesList.add(deviceInfo); //Agregamos la información del dispositivo al arreglo devicesList
                    devicesAdapter.notifyDataSetChanged(); //El adaptador muestra en la lista del layout el nuevo dispositivo
                }
            }

            //Si el intento arroja ACTION_DISCOVERY_STARTED significa que el descubrimiento de dispositivos ha comenzado
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                Toast.makeText(context, "Buscando dispositivos",
                        Toast.LENGTH_LONG).show(); //Mostramos un mensaje en pantalla
            }

            //Si el intento arroja ACTION_DISCOVERY_FINISHED significa que el descubrimiento de dispositivos ha finalizado
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                searchBluetoothDevices(); //Hacemos que la búsqueda se repita cada que termina la anterior
            }
        }
    };


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Aquí inicia la aplicación y configuramos todo para que se pueda buscar dispositivos
    //TODO: Este método es el primero que se ejecuta en el flujo de la aplicación

    @Override //Significa que estamos sobreescribiendo el método
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter =
                this.getSystemService(BluetoothManager.class).getAdapter(); //Capturamos el adaptador Bluetooth del teléfono
        locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE); //Capturamos el servicio de ubicación del teléfono

        //Verificamos si el Bluetooth y la Ubicación del teléfono están activadas
        checkAdaptersEnabled();

        ListView listView = findViewById(R.id.device_list); //Capturamos la lista del layout
        devicesList = new ArrayList<>(); //Inicializamos el arreglo que almacenará los dispositivos
        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                devicesList); //Inicializamos el arreglo de adaptadores para la lista del layout
        listView.setAdapter(devicesAdapter); //Establecemos el adaptador en la lista

        //Configuramos el comportamiento del botón de buscar dispositivos
        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            devicesList.clear(); //Al pulsar el botón se borra la lista de dispositivos
            searchBluetoothDevices(); //Comineza la búsqueda de dispositivos
        });

        //Configuramos un filtro para para que podamos monitorear los distintos momentos de la búsqueda
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); //Cuando se encuentran dispositivos
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //Cuando comienza la búsqueda o descubrimiento
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //Cuando finaliza la búsqueda

        registerReceiver(receiver, filter); //Configuramos el recibidor con el filtro
    }

    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: La función que inicia la búsqueda de dispositivos
    //TODO: Este método se ejecuta cuanto pulsamos el botón buscar dispositivos

    @SuppressLint("MissingPermission") //Desactivamos las sugerencias de código innecesarias
    private void searchBluetoothDevices() {

        //Verificamos si el Bluetooth y la Ubicación del teléfono están activadas
        if (checkAdaptersEnabled()) {
            //Verificamos si nos falta algún permiso para usa el Bluetooth del teléfono
            if (checkPermissions()) {
                requestPermissions(); //Si es así solicitamos que se active el permiso
            } else {
                bluetoothAdapter.startDiscovery(); //Si por el contrario, todos los permisos están activados, comenzamos la búsqueda
            }
        }
    }


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Este método verifica si el Bluetooth y la Ubicación del teléfono están activadas
    //TODO: Este método se ejecuta al iniciar la aplicación y cada que se va a hacer una búsqueda de dispositivos

    private boolean checkAdaptersEnabled(){
        //Creamos unas variables para almacenar si los dispositivos están activados o no
        Boolean bluetoothEnabled = true;
        Boolean locationEnabled = true;

        //Verificamos si el Bluetooth NO está activado, de ser así mostramos un mensaje
        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(this, "Por favor activa el Bluetooth", Toast.LENGTH_LONG).show();
            bluetoothEnabled = false;
        }

        //Verificamos si la ubicación NO está activada, de ser así mostramos un mensaje
        if(!locationManager.isLocationEnabled()){
            Toast.makeText(this, "Por favor activa la ubicación", Toast.LENGTH_LONG).show();
            locationEnabled = false;
        }

        //Si alguno de los dispositivos no está activado, retornamos false
        return (bluetoothEnabled && locationEnabled);
    }

    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Este método simplemente devuelve (true) si falta algún permiso para utilizar el Bluetooht del teléfono
    //TODO: Este método se ejecuta cuando es llamado en ciertas partes del código

    private boolean checkPermissions() {
        return (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.BLUETOOTH_SCAN) //Validamos si falta el permiso de escanear dispositivos
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) //Validamos si falta el permiso de acceso a la ubicación
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) //Validamos si falta el permiso de buscar dispositivos cercanos
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.BLUETOOTH_CONNECT) //Validamos si falta el permiso para conectarse a dispositivos bluetooth
                        != PackageManager.PERMISSION_GRANTED
        );
    }


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Este método solicita todos los permisos para utilizar el Bluetooht del teléfono
    //TODO: Este método se ejecuta cuando es llamado en ciertas partes del código

    private void requestPermissions () {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        android.Manifest.permission.BLUETOOTH_SCAN, //Permiso de escanear dispositivos Bluetooth
                        android.Manifest.permission.ACCESS_FINE_LOCATION, //Permiso para acceder a la ubicación del teléfono
                        android.Manifest.permission.ACCESS_COARSE_LOCATION, //Permiso para encontrar dispositivos cercanos
                        android.Manifest.permission.BLUETOOTH_CONNECT, //Permiso para contectarse a dispositivos Bluetooth
                }, PERMISSION_OK); //Esta variable simplemente vale 1, pero se entiende más claramente que se está activando todos los permisos
    }


    //TODO: --------------------------------------------------------------------------------------------------
    //TODO: Cuando la Activity o aplicación finalice, cancelamos el proceso de busqueda de dispositivos
    //TODO: Este método es el último que se ejecuta en el flujo de la aplicación
    protected void onDestroy() {
        super.onDestroy();

        //Verificamos que el adaptador Bluetooth esté funcionando
        if (bluetoothAdapter != null) {
            //Validamos si el permiso de escanear dispositivos está habilitado
            //Si no está habilitado no es necesario cancelar nada, y si lo hiciéramos la aplicación daría error
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery(); //Si tenemos el permiso entonces sí tenemos que cancelar la búsqueda
            }
        }
        unregisterReceiver(receiver); //Deshabilitamos el recibidor y ya no enviará eventos para monitorear
    }
}