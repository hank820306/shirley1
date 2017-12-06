package mobile.android.demo.bluetooth.chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;
import java.util.Calendar;

import java.util.ArrayList;
import java.util.HashMap;
import android.widget.SimpleAdapter;
import android.view.ContextMenu;  
import android.view.ContextMenu.ContextMenuInfo;  
import android.view.View.OnCreateContextMenuListener; 
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.widget.ListView;
import android.widget.AdapterView.*;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.view.View;

//
//INTERNET

//import java.io.IOException;
//import org.apache.http.HttpResponse; 
//import org.apache.http.client.ClientProtocolException; 
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient; 

//import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//import org.apache.http.client.ResponseHandler;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.BasicResponseHandler;
//import org.apache.http.impl.client.DefaultHttpClient;
import java.net.URLEncoder;
//import java.net.MalformedURLException;

//import org.apache.http.util.EntityUtils; 
public class BluetoothChat extends Activity
{
	//private static final String TAG = "Debug";
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	public static String DataCodes = ""; //存放接收的資料
	public static byte[] DatareadBuf; //存放資料
	public static int msgid; //存放接收的資料
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private TextView mTitle;
	//private Button mDisconnect;
	private Button mOutputButton;
	private CheckBox Automaticcheckbox;
	private String mConnectedDeviceName = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothChatService mChatService = null;
	private ListView list;
	private ArrayList<HashMap<String, Object>> listItem;
	private SimpleAdapter listItemAdapter;
	//test
	//private ProcessHandler ProcessHandler = new ProcessHandler();
	private httpProcessHandler httpProcessHandler = new httpProcessHandler();
	private cProcessHandler cProcessHandler = new cProcessHandler();
	//test
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.custom_title);
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			Toast.makeText(this, "目前手機不支援藍芽.",Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		list = (ListView) findViewById(R.id.in);
		listItem = new ArrayList<HashMap<String, Object>>();  
        listItemAdapter = new SimpleAdapter(this,listItem,
                R.layout.newmsg,
                new String[] {"AutoSend","ItemTitle", "ItemText","ItemMachine"},
                new int[] {R.id.AutoSend,R.id.ItemTitle,R.id.ItemText,R.id.ItemMachine}
        );
        //test
        list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("上傳資料");
                //menu.add(0, 0, 0, "立即");
                menu.add(0, 1, 0, "取消");
            }
        });
	}
	@Override
	public void onStart()
	{
		super.onStart();
		if (!mBluetoothAdapter.isEnabled())
		{
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			if (mChatService == null)
				setupChat();
		}
	}
	@Override
	public synchronized void onResume()
	{
		super.onResume();
	}
	private void setupChat()
	{
		Automaticcheckbox = (CheckBox)findViewById(R.id.checkBox1);
		OnClickListener ocl = new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Toast.makeText(getApplicationContext(), Automaticcheckbox.isChecked() + "", Toast.LENGTH_SHORT).show();
				if(Automaticcheckbox.isChecked()){
					new Thread(){
		                @Override
		                public void run() {
		                	//mClearButton.setEnabled(false);
		                	while(Automaticcheckbox.isChecked()){
		                		while(listItemAdapter.getCount() > 0 && Automaticcheckbox.isChecked()){
			                		//Calculation.calculate(1);
			                		//int dc = listItemAdapter.getCount()-1;
			                		int dc = 0;
			                		Message pu = new Message();
									pu.what = 1;
									pu.arg1 = dc;
									pu.arg2 = 1;
				                    cProcessHandler.sendMessage(pu);
				                    try{
				                		HashMap<String, Object> mapr = (HashMap)list.getItemAtPosition(dc);
					                	String xdata = (String)mapr.get("ItemText");
					                	String mdata = (String)mapr.get("ItemMachine");
					                	if(httpProcessHandler.printMessage("http://192.168.1.140/blood/setdt.php?data=" + URLEncoder.encode(xdata) + "&machine=" + URLEncoder.encode(mdata) + "&mdate=" + URLEncoder.encode(rtime()))){
					                		Message pn = new Message();
											pn.what = 0;
											pn.arg1 = dc;
						                    cProcessHandler.sendMessage(pn);
						                    Calculation.calculate(1);
					                	}else{
					                		Message pq = new Message();
											pq.what = 2;
											pq.arg1 = dc;
						                    cProcessHandler.sendMessage(pq);
						                    Calculation.calculate(1);
						                    break;
					        			}
				                    }catch(Exception e){
				                    	break;
				                    }
			                	}
		                	}
		                	
		                	/*Message pr = new Message();
							pr.arg2 = 2;
							pr.what = 3;
							Calculation.calculate(1);
		                    cProcessHandler.sendMessage(pr);*/
		                }
	            	}.start();
				}
			}
		};
		Automaticcheckbox.setOnClickListener(ocl);
		/*
		Automaticcheckbox.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				new Thread(){
	                @Override
	                public void run() {
	                	//mClearButton.setEnabled(false);
	                	while(listItemAdapter.getCount() > 0){
	                		//Calculation.calculate(1);
	                		//int dc = listItemAdapter.getCount()-1;
	                		int dc = 0;
	                		Message pu = new Message();
							pu.what = 1;
							pu.arg1 = dc;
							pu.arg2 = 1;
		                    cProcessHandler.sendMessage(pu);
		                    try{
		                		HashMap<String, Object> mapr = (HashMap)list.getItemAtPosition(dc);
			                	String xdata = (String)mapr.get("ItemText");
			                	String mdata = (String)mapr.get("ItemMachine");
			                	if(httpProcessHandler.printMessage("http://192.168.1.140/blood/setdt.php?data=" + URLEncoder.encode(xdata) + "&machine=" + URLEncoder.encode(mdata) + "&mdate=" + URLEncoder.encode(rtime()))){
			                		Message pn = new Message();
									pn.what = 0;
									pn.arg1 = dc;
				                    cProcessHandler.sendMessage(pn);
				                    Calculation.calculate(1);
			                	}else{
			                		Message pq = new Message();
									pq.what = 2;
									pq.arg1 = dc;
				                    cProcessHandler.sendMessage(pq);
				                    Calculation.calculate(1);
				                    break;
			        			}
		                    }catch(Exception e){
		                    	break;
		                    }
	                	}
	                	Message pr = new Message();
						pr.arg2 = 2;
						pr.what = 3;
						Calculation.calculate(1);
	                    cProcessHandler.sendMessage(pr);
	                }
            	}.start();
            	
			}
		});
		*/
		mOutputButton = (Button) findViewById(R.id.button_output);
		mOutputButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				HashMap<String, Object> map = new HashMap<String, Object>();
				map.put("AutoSend", "");
		        map.put("ItemTitle", "血壓資訊");
		        map.put("ItemText", "收縮壓=100,舒張壓=100,心跳=100");
		        map.put("ItemMachine", "TestMachine");
		        listItem.add(map);
		        list.setAdapter(listItemAdapter);
			}
		});
		/*mDisconnect = (Button) findViewById(R.id.button_disconnect);
		mDisconnect.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (mChatService != null)
					mChatService.stop();
			}
		});*/
		mChatService = new BluetoothChatService(this, mHandler);
	}
	@Override
	public synchronized void onPause()
	{
		super.onPause();
		if (mChatService != null)
			mChatService.stop();
	}
	@Override
	public void onStop()
	{
		super.onStop();
		if (mChatService != null)
			mChatService.stop();
	}
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (mChatService != null)
			mChatService.stop();
	}
	private void ensureDiscoverable()
	{
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
		{
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
	private final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			//list.setAdapter(listItemAdapter);
			//取血壓資訊
			int firstPosition = DataCodes.indexOf("#ORG");
			int lastPosition = DataCodes.indexOf("##########");
			if(firstPosition != -1 && lastPosition != -1){
				String substring = DataCodes.substring(firstPosition+6, lastPosition);
				int HPosition = substring.indexOf("H=");
				int LPosition = substring.indexOf("L=");
				int PPosition = substring.indexOf("P=");
				if(HPosition != -1 && LPosition != -1 && PPosition != -1){
					String outputS = DataCodes.substring(firstPosition+6, lastPosition);
					if(outputS.length() >= 35 || outputS.length() <= 10){
						Toast.makeText(getApplicationContext(),"血壓機訊號錯誤，請重新量測!!", Toast.LENGTH_SHORT).show();
						DataCodes = "";
					}else{
						outputS = outputS.replace("H=", "收縮壓=");
						outputS = outputS.replace("L=", "舒張壓=");
						outputS = outputS.replace("P=", "心跳=");
						//Toast.makeText(getApplicationContext(),"血壓機的訊號有進來!!", Toast.LENGTH_SHORT).show();
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put("AutoSend", "");
				        map.put("ItemTitle", "血壓資訊");
				        map.put("ItemText", outputS);
				        map.put("ItemMachine", mConnectedDeviceName);
				        listItem.add(map);
				        list.setAdapter(listItemAdapter);
				        DataCodes = "";
				        //myVibrator.vibrate(1000);
				        //倒數計時
				        /*
				        msgid = listItem.lastIndexOf(map);
				        new Thread(){
				        	public int mapid;
			                @Override
			                public void run() {
			                	mapid = msgid;
			                	for(int i=30;i>=0;i--){
			                		Message pmsg = new Message();
			    					pmsg.what = i;
			    					pmsg.arg1 = mapid;
				                    ProcessHandler.sendMessage(pmsg);
			    					Calculation.calculate(1);
			                	}
			                }
				        }.start();
				        //倒數計時
				        */
					}
				}
			}
			//取血壓資訊
			switch (msg.what)
			{
				case MESSAGE_STATE_CHANGE:
					switch (msg.arg1)
					{
						case BluetoothChatService.STATE_CONNECTED:
							mTitle.setText(R.string.title_connected_to);
							mTitle.append(mConnectedDeviceName);
						break;
						case BluetoothChatService.STATE_CONNECTING:
							mTitle.setText(R.string.title_connecting);
						break;
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							mTitle.setText(R.string.title_not_connected);
						break;
					}
				break;
				case MESSAGE_READ:
					DataCodes += msg.obj;
				break;
				case MESSAGE_DEVICE_NAME:
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(),"Connected to " + mConnectedDeviceName,Toast.LENGTH_SHORT).show();
				break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(),msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			}
			
		}
	};
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CONNECT_DEVICE:
				if (resultCode == Activity.RESULT_OK)
				{
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					mChatService.connect(device);
				}
				break;
			case REQUEST_ENABLE_BT:
				if (resultCode == Activity.RESULT_OK)
				{
					setupChat();
				}
				else
				{
					Toast.makeText(this, R.string.bt_not_enabled_leaving,Toast.LENGTH_SHORT).show();
					finish();
				}
			break;
		}
	}
	public String rtime()
	{
		int mHour; 
		int mMinute; 
		int mYear; 
		int mMonth; 
		int mDay;
		int mSecond;
		Calendar c = Calendar.getInstance(); 
	    mYear = c.get(Calendar.YEAR);
	    mMonth = c.get(Calendar.MONTH);
	    mDay = c.get(Calendar.DAY_OF_MONTH);
	    mHour = c.get(Calendar.HOUR);
	    mMinute = c.get(Calendar.MINUTE);
	    mSecond = c.get(Calendar.SECOND);
		return mYear + "-" + mMonth + "-" + mDay + " " + mHour + ":" + mMinute + ":" + mSecond;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.scan:
				Intent serverIntent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				return true;
			case R.id.discoverable:
				ensureDiscoverable();
				return true;
		}
		return false;
	}
	@Override  
    public boolean onContextItemSelected(MenuItem item) {
		int selectedPosition = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
		if(item.getItemId() == 1){//取消資料
			listItem.remove(selectedPosition);
	    	list.setAdapter(listItemAdapter);
	    	//Toast.makeText(getApplicationContext(), selectedPosition,Toast.LENGTH_SHORT).show();
		}
		if(item.getItemId() == 0){//立即上傳
			/*
			new Thread(){
                @Override
                public void run() {
                }
        	}.start();
        	*/
		}
		return super.onContextItemSelected(item);  
    }
	class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            HashMap<String, Object> maps = (HashMap)list.getItemAtPosition(msg.arg1);
            if(msg.what == 0){
            	maps.put("AutoSend", "上傳中");
            }else{
            	maps.put("AutoSend", msg.what);
            }
            listItem.set(msg.arg1, maps);
            list.setAdapter(listItemAdapter);
        }
    }
	class cProcessHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	if(msg.what == 0){
        		listItem.remove(msg.arg1);
        	}
        	if(msg.what == 1){
        		HashMap<String, Object> mapr = (HashMap)list.getItemAtPosition(msg.arg1);
        		mapr.put("AutoSend", "上傳中");
        		listItem.set(msg.arg1, mapr);
        	}
        	if(msg.what == 2){
        		HashMap<String, Object> mapr = (HashMap)list.getItemAtPosition(msg.arg1);
        		mapr.put("AutoSend", "上傳失敗，請檢查網路後再重試");
        		listItem.set(msg.arg1, mapr);
        	}
        	if(msg.arg2 == 1){
        		//mClearButton.setEnabled(false);
        	}
        	if(msg.arg2 == 2){
        		//mClearButton.setEnabled(true);
        	}
        	try{
        		list.setAdapter(listItemAdapter);
        	}catch(Exception e){
            	return ;
            }
        }
    }
	class httpProcessHandler extends Handler {
        private boolean printMessage(String httpgeturl)
        {
        	try
            {
            	URL url = new URL(httpgeturl);
    			HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
    			if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
    				return true;
    			}else{
    				return false;
    			}
            }
            catch (Exception e) 
            {
            	return false;
            }
        }
    }
	public boolean Upload_Data(String uriAPI){
        try
        {
        	URL url = new URL(uriAPI);
			HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
			if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return true;
			}else{
				return false;
			}
        }
        catch (Exception e) 
        {
          return false;
        }
    }
}
class Calculation extends Handler {
    public static void calculate(int sleepSeconds){
        try {
            Thread.sleep(sleepSeconds * 1000);
        } catch (Exception e) {}
    }
}
