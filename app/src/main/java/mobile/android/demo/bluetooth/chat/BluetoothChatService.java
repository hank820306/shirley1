package mobile.android.demo.bluetooth.chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothChatService
{	
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;
	private static final String NAME = "BluetoothChat";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1; 
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3; 
	public BluetoothChatService(Context context, Handler handler)
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}
	private synchronized void setState(int state)
	{
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;
		mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}
	public synchronized int getState()
	{
		return mState;
	}

	public synchronized void start()
	{
		if (D)
			Log.d(TAG, "start");

		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread == null)
		{
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN);
	}
	public synchronized void connect(BluetoothDevice device)
	{
		if (D)
			Log.d(TAG, "connect to: " + device);

		if (mState == STATE_CONNECTING)
		{
			if (mConnectThread != null)
			{
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device)
	{
		if (D)
			Log.d(TAG, "connected");

		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null)
		{
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		setState(STATE_CONNECTED);
	}
	public synchronized void stop()
	{
		if (D)
			Log.d(TAG, "stop");
		if (mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null)
		{
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	public void write(byte[] out)
	{
		ConnectedThread r;
		synchronized (this)
		{
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		r.write(out);
	}

	private void connectionFailed()
	{
		setState(STATE_LISTEN);

		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private void connectionLost()
	{
		setState(STATE_LISTEN);

		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private class AcceptThread extends Thread
	{
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread()
		{
			BluetoothServerSocket tmp = null;

			try
			{
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			}
			catch (IOException e)
			{
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run()
		{
			if (D)
				Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			while (mState != STATE_CONNECTED)
			{
				try
				{
					socket = mmServerSocket.accept();
				}
				catch (IOException e)
				{
					Log.e(TAG, "accept() failed", e);
					break;
				}

				if (socket != null)
				{
					synchronized (BluetoothChatService.this)
					{
						switch (mState)
						{
							case STATE_LISTEN:
							case STATE_CONNECTING:
								connected(socket, socket.getRemoteDevice());
								break;
							case STATE_NONE:
							case STATE_CONNECTED:
								try
								{
									socket.close();
								}
								catch (IOException e)
								{
									Log.e(TAG,
											"Could not close unwanted socket",
											e);
								}
								break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread");
		}

		public void cancel()
		{
			if (D)
				Log.d(TAG, "cancel " + this);
			try
			{
				mmServerSocket.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device)
		{
			mmDevice = device;
			BluetoothSocket tmp = null;

			try
			{
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			}
			catch (IOException e)
			{
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run()
		{
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			mAdapter.cancelDiscovery();

			try
			{
				mmSocket.connect();
			}
			catch (IOException e)
			{
				connectionFailed();
				try
				{
					mmSocket.close();
				}
				catch (IOException e2)
				{
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				BluetoothChatService.this.start();
				return;
			}

			synchronized (BluetoothChatService.this)
			{
				mConnectThread = null;
			}
			connected(mmSocket, mmDevice);
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket)
		{
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch (IOException e)
			{
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run()
		{
			//Log.i(TAG, "BEGIN mConnectedThread");
			/*
			byte[] buffer = new byte[4096];
			int bytes;
			while (true)
			{
				try
				{
					bytes = mmInStream.read(buffer);
					mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes,-1, buffer).sendToTarget();
				}
				catch (IOException e)
				{
					//Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
			*/
			
			
			//Log.i(TAG, "BEGIN mConnectedThread");
		    int bufferSize = 4096;
		    byte[] buffer = new byte[bufferSize];
		    // Keep listening to the InputStream while connected
		    while (true) {
		    	//Log.d(TAG, "a");
	            int bytesRead = -1;
	            String message = "";
		        try {
		            // Read from the InputStream
		            if (mmInStream.available() > 0) {
		            	bytesRead = mmInStream.read(buffer);
		                if (bytesRead > 0) {
		                    while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
		                    	//Log.d(TAG, "b");
		                        message = message + new String(buffer, 0, bytesRead);
		                        bytesRead = mmInStream.read(buffer);
		                    }
		                    if((buffer[bytesRead - 1] != 0)) {
		                        message += new String(buffer, 0, bytesRead);
		                    } else {
		                        message += new String(buffer, 0, bytesRead - 1);
		                    }
		                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, message.getBytes().length, -1, message).sendToTarget();
		                }
		            }
		        } catch (IOException e) {
		        	connectionLost();
					break;
		        }
		    }
		    
		}
		public void write(byte[] buffer)
		{
			try
			{
				mmOutStream.write(buffer);
				mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1,
						buffer).sendToTarget();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel()
		{
			try
			{
				mmSocket.close();
			}
			catch (IOException e)
			{
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}