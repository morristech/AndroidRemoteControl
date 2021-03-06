package com.i2r.androidremotecontroller;

import java.io.File;
import java.util.UUID;

import ARC.Constants;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.i2r.androidremotecontroller.connections.BluetoothLink;
import com.i2r.androidremotecontroller.connections.ConnectionManager;
import com.i2r.androidremotecontroller.exceptions.NoBluetoothAdapterFoundException;
import com.i2r.androidremotecontroller.sensors.SensorController;

/**
 * This class models the master to pivot all sub-operations of this
 * application on. Everything involving state change, creation and
 * destruction filters through an instance of this object.
 * @author Josh Noel
 */
public class RemoteControlMaster {
	
	private static final String TAG = "RemoteControlMaster";
	
	private ConnectionManager connectionManager;
	private SensorController sensorController;
	private boolean started;
	
	
	/**
	 * Constructor
	 * creates a new master to control the flow of command reads and execution.
	 * @param activity - the activity to send broadcasts with so that this
	 * hierarchy will waterfall correctly.
	 * @param holder - a holder to give to the command manager for image capturing
	 */
	public RemoteControlMaster(SensorController sensors){
		
		this.sensorController = sensors;
		this.started = false;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		
		try {
			
			// create a BluetoothLink to pass to this ConnectionManager
			BluetoothLink linker = new BluetoothLink(adapter,
					UUID.fromString(Constants.Info.UUID),
					Constants.Info.SERVICE_NAME, sensors.getRelativeActivity());
			
			// create a new ConnectionManager
			this.connectionManager = new ConnectionManager(linker,
					ConnectionManager.CONNECTION_TYPE_SERVER, sensors.getRelativeActivity());
			
			Log.d(TAG, "connection manager created");
			
			// bluetooth is not supported on this device
		} catch (NoBluetoothAdapterFoundException e) {
			connectionManager = null;
			Log.e(TAG, "connection manager creation failed");
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Initializer used by the main activity
	 */
	public void start(){
		this.started = true;
		if(!connectionManager.hasConnection()){
			Log.d(TAG, "finding connections with connection manager");
			connectionManager.findConnection();
		}
	}
	
	
	/**
	 * Used by the main activity to stop this application
	 */
	public void stop(){
		Log.d(TAG, "master stopped");
		this.started = false;
	
		if(connectionManager != null){
			connectionManager.cancel();
		}
		
		if(sensorController != null){
			sensorController.cancel();
			sensorController.setConnection(null);
		}
	}
	
	

	
	
	/**
	 *  if the command manager has not been created due to no available
	 *  RemoteConnection, then create a new CommandManager
	 */
	public void initializeConnection(){
		
		// if there is a connection established and the command manager is null,
		// create a new CommandManager
		if(connectionManager.hasConnection() && started){
			
			// check the remote connection for any new incoming data
			connectionManager.startDataTransfer();
			
			Log.d(TAG, "connection found, passing reference to responder");
			this.sensorController.setConnection(connectionManager.getConnection());
			
			// if there is no connection established and the connection manager
			// isn't running, try again to get a remote connection
		} else if (!connectionManager.hasConnection() && started){
			
			Log.d(TAG, "no connection found, restarting connection manager");
			connectionManager.findConnection();
		}
	}
	
	
	
	
	/**
	 * Any sensor updates or new command reads are thrown to the
	 * main activity which then waterfalls down to this method to
	 * update the command manager. 
	 */
	public synchronized void updateByRemoteControl(String command){	

		Log.d(TAG, "starting update loop");
		// if a remote connection has been established, continue with update
		if (connectionManager.hasConnection() && started) {
			
			Log.d(TAG, "parsing new command...");
			sensorController.parseCommand(command);

			if(sensorController.canExecuteNextCommand()){
				Log.d(TAG, "executing next command...");
				sensorController.executeNextCommand();
			}
			
			// cannot update commands because there is no connection established
		} else if(!connectionManager.hasConnection() && started){
			Log.d(TAG, "connection lost, restarting connection manager.");
			connectionManager.findConnection();
			
		} else {
			
			// alert the System to scan for new files are on the SD card
			File file = new File(Environment.getExternalStoragePublicDirectory(
				    Environment.DIRECTORY_PICTURES).getAbsolutePath());
				sensorController.getRelativeActivity()
				.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
				    Uri.parse("file://"+ file)));
		}
	}
	
	
} // end on RemoteControlMaster class
