package com.mlab.transmitter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class TransmitterActivity extends Activity implements SensorEventListener{
    public static final String LOGTAG = "SierraWhiskey";
    SensorManager sm;
    Sensor AccelSensor;

    private int WIFI = 0;
    private int BT = 1;
    private int phyType = WIFI;

    private int screenWidth;
    private int screenHeight;

    AlertDialog aDailog;
    DataProcess dataProcess;
    UiProcess   uiProcess  ;
    QMainMenu mainDialog;

	/* Support Bluetooth */
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
//    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private int config_motor_int = 0;
    private boolean config_use_accel = false;
    private int config_mod_int = 0;	// 0: Bronto, 1: MyFlyer
    private int key_accel, ex_key_accel= 0;
    private MyView mView;
    private boolean connection_status = false;
    private int connect_seq = 0;

    private short acc_x, acc_y, acc_z = 0;
//    private LinearLayout mLayout;
    private Thread background_thread = null;
    private boolean bThread_Active = false;
    private int exMotorCmd = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(LOGTAG,"onCreate - AccelSensor");

		//mLayout = (LinearLayout) findViewById(R.id.mainlayout);
		//mLayout.setBackgroundColor(Color.rgb(255,255,0));

		sm = (SensorManager)getSystemService(SENSOR_SERVICE);
		AccelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sm.unregisterListener(TransmitterActivity.this, AccelSensor);
		sm.registerListener(TransmitterActivity.this, AccelSensor, SensorManager.SENSOR_DELAY_UI);

		exMotorCmd = -1;

		if(phyType == WIFI)	{
			Log.d(LOGTAG,"onCreate - DataProcess");
			dataProcess=new DataProcess(hMsg,this);
			Log.d(LOGTAG,"onCreate - UiProcess");
			uiProcess  =new UiProcess(this,hMsg,dataProcess);
			Log.d(LOGTAG,"onCreate - UiProcess done.");
	        createDialog();
	        mainDialog=new QMainMenu(this);
	        //mainDialog.setOnItemClickListener(new onMenuItemClick());
	        //uiProcess.createConfigWindow=uiProcess.createConfigWindow();
		} else if(phyType == BT)	{
			//setContentView(R.layout.main);

        // Get local Bluetooth adapter
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
	    }

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenWidth = metrics.widthPixels;
		screenHeight = metrics.heightPixels;
		Log.d(LOGTAG,"screenWidth:"+screenWidth+"screenHeight:"+screenHeight);
		mView = new MyView(this);
		setContentView(mView);

		//background_thread();
		//bThread_Active = true;
		//background_thread.start();
  	    Log.d(LOGTAG,"onCreate");

	}

    private void background_thread() {
    	background_thread = new Thread() {

			public void run(){
        		try{
        			while (bThread_Active){
					if(exMotorCmd > 0)
						uiProcess.genStick(0xF0, exMotorCmd);
					Log.d(LOGTAG, "background_thread[debug] exMotorCmd[0]:"+(exMotorCmd>>16)+", exMotorCmd[1]:"+(exMotorCmd&0xFFFF)); // -49 ~ 50 range.
					Thread.sleep(500);
        			}
        		}catch(Exception e){
        			;
        		}
        	}
        };
    }

    @Override
    public void onStart() {
        super.onStart();
	 exMotorCmd = -1;
        Log.e(LOGTAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if(phyType == BT)	{
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        // Otherwise, setup the chat session
	        } else {
	            if (mChatService == null) setupChat();
	        }
        }
    }

	@Override
	public synchronized void onResume() {
		super.onResume();
		exMotorCmd = -1;
		Log.e(LOGTAG, "+ ON RESUME +");
		
		bThread_Active = true;
		background_thread();
		background_thread.start();
		
///////////////////////////////////////////////////////////////
		SharedPreferences prefs = getSharedPreferences("Configuration", MODE_PRIVATE);
		//config_motor = prefs.getString("MotorStrength", "");
		config_motor_int = prefs.getInt( "MotorStrengthInt", 100);
		config_use_accel = prefs.getBoolean( "Enable_accel", false);
		config_mod_int = prefs.getInt( "Mode Config", 0);
		//Log.d(LOGTAG,"onResume - MotorStrength: ["+ config_motor_int+"] Enable_accel: ["+config_use_accel+"]"+"] Mode Config: ["+config_mod_int+"]");
///////////////////////////////////////////////////////////////
		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
			  // Start the Bluetooth chat services
			  mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(LOGTAG, "setupChat()");
/*
		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		Log.d(LOGTAG, "setupChat()1");

		// Initialize the compose field with a listener for the return key
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		Log.d(LOGTAG, "setupChat()2");

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);
			}
		});
		Log.d(LOGTAG, "setupChat()3");
*/
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		Log.d(LOGTAG, "setupChat()4");

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		bThread_Active = false;
		background_thread = null;
		Log.e(LOGTAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.e(LOGTAG, "-- ON STOP --");
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        bThread_Active = false;

        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        Log.e(LOGTAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
    	Log.e(LOGTAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            Log.e(LOGTAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(LOGTAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOGTAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(LOGTAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.option_menu, menu);	// BT
        inflater.inflate(R.menu.menu_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        case R.id.configuration:
		Intent intent = new Intent(TransmitterActivity.this, Configuration.class);
		//intent.putExtra("MUTE_STATUS", IsMute);
		//startActivityForResult(intent, REQUEST_CODE_1);
		startActivity(intent);
            return true;

        }
        return false;
    }

	HandleMsg hMsg=new HandleMsg(){
		@Override
		public void handleMessage(Message msg) {
			/*Calendar c = Calendar.getInstance();
			int milliseconds = c.get(Calendar.MILLISECOND);
			int seconds = c.get(Calendar.SECOND);
			int minutes = c.get(Calendar.MINUTE);
			int hours = c.get(Calendar.HOUR);*/
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			//if(phyType==WIFI)	return;	// This made problem.
			//Log.d(LOGTAG,"TransmitterActivity handleMessage msg.what:"+msg.what);
			if(dataProcess==null) return;

			if(msg.what==dataProcess.RELAYOPT)
			{
				dataProcess.sendrelayCmd(msg.arg1,msg.arg2);

			}
			else if(msg.what==dataProcess.RELAYCHK)		// check
			{
				if(ckeck)
				{
					dataProcess.sendrelayCmd(5,0x17c517c5);
					this.sendEmptyMessageDelayed(dataProcess.RELAYCHK, 500);
				}
			}
			else if(msg.what==dataProcess.CLOSETCP)
			{
				uiProcess.stopConn();
			}
			else if(msg.what==dataProcess.RELAYSTATE)
			{
				uiProcess.setRelayState(msg.arg1);
			}
			else if(msg.what==dataProcess.APPQUIT)
			{
				aDailog.show();
			}
			else if(msg.what==dataProcess.MAINMENU)
			{
				mainDialog.show();
			}
			else if(msg.what==dataProcess.GET_REMOTE_INFO)
			{

				if ((msg.arg1 & 0xFF) == 6)	{
					acc_x = (short) (msg.arg2 >> 16);
					acc_y = (short) (msg.arg2 & 0xFFFF);
					acc_z = (short) (msg.arg1 >> 16);
					/*Log.d(LOGTAG," ID:"+(msg.arg1 & 0xFF)
							+", msg.arg2[0]:"+(msg.arg2 >> 16)
							+", msg.arg2[1]:"+(msg.arg2 & 0xFFFF)
							+", msg.arg2[2]:"+(msg.arg1 >> 16));*/
					mView.invalidate();
				}
				connect_seq ++;
				msg=new Message();
				msg.what=dataProcess.CHECK_CONNECTION;
				msg.arg1= connect_seq;
				this.sendMessageDelayed(msg, 1000);

				if(!connection_status)	{
					connection_status = true;
					mView.invalidate();
				}

			}
			else if(msg.what==dataProcess.CHECK_CONNECTION)
			{
				if(connect_seq - msg.arg1 <4)
					Log.d(LOGTAG,"CHECK_CONNECTION msg.arg1:"+msg.arg1+", seq2:"+connect_seq);
				if(connection_status && (msg.arg1 == connect_seq))	{
					connection_status = false;
					mView.invalidate();
				}
			}

		}
	};

	public void createDialog()
    {
		Log.d(LOGTAG,"TransmitterActivity createDialog");

        aDailog=   new AlertDialog.Builder(TransmitterActivity.this)
        						.setTitle("TEST")
        						.setNegativeButton("TEST2",  new DialogInterface.OnClickListener(){
        							public void onClick(DialogInterface dialoginterface, int i){
        								aDailog.dismiss();
        							}})
        						.setPositiveButton("TEST3",new DialogInterface.OnClickListener(){
        							public void onClick(DialogInterface dialoginterface, int i){
        								uiProcess.saveIpPort();
        								Intent home = new Intent(Intent.ACTION_MAIN);
        								home.addCategory(Intent.CATEGORY_HOME);
        								TransmitterActivity.this.startActivity(home);
        								try {
        									Thread.sleep(800);
        								} catch (InterruptedException e) {
        									e.printStackTrace();
        								}



        								System.exit(0);

        							}})
        						 .create() ;


		 aDailog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				Log.d(LOGTAG,"TransmitterActivity onKey keyCode:"+keyCode+", event:"+event);

				if((event.ACTION_DOWN==event.getAction())&&(event.getRepeatCount()==0))
				{
					if(keyCode==event.KEYCODE_BACK)
					{
		 				Intent home = new Intent(Intent.ACTION_MAIN);
						home.addCategory(Intent.CATEGORY_HOME);
						TransmitterActivity.this.startActivity(home);
						/**/	try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
					}
					return true;
				}
				return false;
			}
		});
    }

	public class MyView extends View{

		private int pointer_counter;
		private PointF[] point;
		private Paint p;
		private static final int MAX_POINTERS=2;

		private int SCREEN_X = screenWidth;
		private int SCREEN_Y = screenHeight;

		private int STICK_CIRCLE_R=screenWidth/6;
		private int STICK_CIRCLE_RR=STICK_CIRCLE_R*STICK_CIRCLE_R;
		private int STICK_CENTER_Y=screenHeight/2;
		private int STICK_CENTER_X1=screenWidth/4;
		private int STICK_CENTER_X2=screenWidth*3/4;
		private int STICK_CENTER_X=(int)((STICK_CENTER_X1+STICK_CENTER_X2)/2);

		private int STICK_X1_MIN=STICK_CENTER_X1-STICK_CIRCLE_R;
		private int STICK_X1_MAX=STICK_CENTER_X1+STICK_CIRCLE_R;
		private int STICK_X2_MIN=STICK_CENTER_X2-STICK_CIRCLE_R;
		private int STICK_X2_MAX=STICK_CENTER_X2+STICK_CIRCLE_R;
		private int STICK_Y_MIN=STICK_CENTER_Y-STICK_CIRCLE_R;
		private int STICK_Y_MAX=STICK_CENTER_Y+STICK_CIRCLE_R;

		private int STICK_FINGER_SENSE=screenHeight/10;
		private int STICK_FINGER=screenHeight/15;

		private int NUM_STICK_L=0;
		private int NUM_STICK_R=0;
		private int cont_x, cont_y, cont_z, cont_w;
		private int ex_cont_x, ex_cont_y, ex_cont_z, ex_cont_w;

		//Log.d(LOGTAG,"screenWidth:"+screenWidth+"screenHeight:"+screenHeight);

		public MyView(Context context) {
			super(context);
			point = new PointF[MAX_POINTERS+10];
		}


		protected void onDraw(Canvas canvas){
			super.onDraw(canvas);
			int result;

			p = new Paint(Paint.ANTI_ALIAS_FLAG);

			p.setColor(Color.LTGRAY);
			canvas.drawCircle(STICK_CENTER_X1, STICK_CENTER_Y, STICK_CIRCLE_R, p);
			canvas.drawCircle(STICK_CENTER_X2, STICK_CENTER_Y, STICK_CIRCLE_R, p);
			p.setColor(Color.BLACK);
			canvas.drawLine(0, STICK_CENTER_Y, SCREEN_X, STICK_CENTER_Y, p);
			canvas.drawLine(STICK_CENTER_X1, 0, STICK_CENTER_X1, SCREEN_Y, p);
			canvas.drawLine(STICK_CENTER_X2, 0, STICK_CENTER_X2, SCREEN_Y, p);
			//p.setColor(Color.YELLOW);
			//canvas.drawLine(STICK_CENTER_X, 0, STICK_CENTER_X, SCREEN_Y, p);
			NUM_STICK_L=0;
			NUM_STICK_R=0;

			if(connection_status)	mView.setBackgroundColor(Color.BLACK);
			else	mView.setBackgroundColor(Color.RED);

			for(int i=0;i<pointer_counter;++i){
				double boundary=0;
				double theta=0;
				double center_x, center_min, center_max;

				if(point[i]!=null)	{
					//p.setColor(Color.WHITE);
					//p.setTextSize(20);
					//canvas.drawText("index: "+i+", x: "+point[i].x+", y:"+point[i].y, 310, ((i+1)*20)+60, p);

					/* Re-location */
					if(point[i].x<STICK_CENTER_X)	{	// LEFT
						center_x = STICK_CENTER_X1;
						center_min = STICK_X1_MIN;
						center_max = STICK_X1_MAX;
						NUM_STICK_L++;
					}
					else	{	// RIGHT
						center_x = STICK_CENTER_X2;
						center_min = STICK_X2_MIN;
						center_max = STICK_X2_MAX;
						NUM_STICK_R++;
					}
					boundary = Math.sqrt(STICK_CIRCLE_RR-Math.pow((point[i].x-center_x), 2));
					if(point[i].y<(STICK_CENTER_Y-boundary) || point[i].y>(STICK_CENTER_Y+boundary)
							|| point[i].x<center_min || point[i].x>center_max	)	{
						theta = Math.atan2(point[i].y-STICK_CENTER_Y, point[i].x-center_x);
						theta = -1*theta-Math.PI/2;
						point[i].x = (float) center_x - (float)Math.sin(theta)*STICK_CIRCLE_R;
						point[i].y = STICK_CENTER_Y - (float)Math.cos(theta)*STICK_CIRCLE_R;
						//theta = Math.toDegrees(theta);
						//Log.d(LOGTAG, "theta:"+theta+"degree.");
					}

					/* Re-location*/

					if(point[i].x<STICK_CENTER_X)	{
						if (config_mod_int == 0)	{
							point[i].x = STICK_CENTER_X1;
						}
						else if (config_mod_int == 1)	{
							point[i].y = STICK_CENTER_Y;
						}
					}
					else	{
						if(config_mod_int == 0)	{
							if(config_use_accel == false)	{
								point[i].y = STICK_CENTER_Y;
							}
						}
						else if (config_mod_int == 1)	{
							point[i].x = STICK_CENTER_X2;
						}
					}
					p = new Paint(Paint.ANTI_ALIAS_FLAG);
					p.setColor(Color.MAGENTA);
					canvas.drawCircle(point[i].x, point[i].y, STICK_FINGER, p);

					/* Control */
					if(point[i].x<STICK_CENTER_X)	{	// LEFT
						cont_x = 50-(int) (point[i].x-center_min)*101/(STICK_CIRCLE_R*2);
						cont_y = 50-(int) (point[i].y-STICK_Y_MIN)*101/(STICK_CIRCLE_R*2);
					}
					else	{	// RIGHT
						cont_z = 50-(int) (point[i].x-center_min)*101/(STICK_CIRCLE_R*2);
						cont_w = 50-(int) (point[i].y-STICK_Y_MIN)*101/(STICK_CIRCLE_R*2);
					}
				}
			}
			if(NUM_STICK_L==0)	{
				cont_x=cont_y=0;
				p = new Paint(Paint.ANTI_ALIAS_FLAG);
				p.setColor(Color.MAGENTA);
				canvas.drawCircle(STICK_CENTER_X1, STICK_CENTER_Y, STICK_FINGER, p);
			}

			if(NUM_STICK_R==0)	{
				cont_z=cont_w=0;
				p = new Paint(Paint.ANTI_ALIAS_FLAG);
				p.setColor(Color.MAGENTA);
				canvas.drawCircle(STICK_CENTER_X2, STICK_CENTER_Y, STICK_FINGER, p);
			}

			if(config_use_accel == true)	{
				p = new Paint(Paint.ANTI_ALIAS_FLAG);
				p.setColor(Color.GREEN);

				if(config_mod_int == 0)		{	// Bronto
					cont_z = key_accel;
					canvas.drawCircle(STICK_X2_MIN + (STICK_CIRCLE_R*2)*(50 - cont_z)/101, STICK_CENTER_Y, STICK_FINGER, p);
				}
				else if(config_mod_int == 1)	{	// MyFlyer
					cont_x = key_accel;
					canvas.drawCircle(STICK_X1_MIN + (STICK_CIRCLE_R*2)*(50 - cont_x)/101, STICK_CENTER_Y, STICK_FINGER, p);
				}

			}

			p.setTextAlign(Paint.Align.CENTER);
			p.setColor(Color.BLACK);
			p.setTextSize(40);
			canvas.drawText(config_motor_int+"%", STICK_CENTER_X1, STICK_CENTER_Y, p);

			p.setColor(Color.WHITE);
			p.setTextSize(40);
			//canvas.drawText("x:"+cont_x+",y:"+cont_y+" ,z:"+cont_z+",w:"+cont_w, SCREEN_X/2, SCREEN_Y/5, p);
			canvas.drawText("x:"+acc_x+",y:"+acc_y+",z:"+acc_z, SCREEN_X/2, SCREEN_Y/5, p);
			//canvas.drawText("x:"+acc_x+",y:"+acc_y, SCREEN_X/2, SCREEN_Y/5, p);
			{
				//config_motor_int	1~100
				int backward_offset = 2;
				if(cont_y < 0 && config_motor_int * backward_offset < 1)
					config_motor_int*=backward_offset;

				cont_y *= (config_motor_int*0.01);
			}
			result = prepare_motor_cmd(cont_x, cont_y, cont_z, cont_w);

			int min_variance = 1;
			if(phyType==WIFI)	{
				if(Math.abs(ex_cont_x-cont_x)>min_variance	||
					Math.abs(ex_cont_y-cont_y)>min_variance	||
					Math.abs(ex_cont_z-cont_z)>min_variance	||
					Math.abs(ex_cont_w-cont_w)>min_variance)	{
					uiProcess.genStick(0xF0, result);
					//Log.d(LOGTAG, "unpackageCmd[debug] reault[0]:"+(result>>16)+", result[1]:"+(result&0xFFFF)); // -49 ~ 50 range.
					ex_cont_x = cont_x;
					ex_cont_y = cont_y;
					ex_cont_z = cont_z;
					ex_cont_w = cont_w;
					exMotorCmd = result;
				}
			} else if (phyType==BT)	{
				sendMessage(Integer.toHexString(result));
				exMotorCmd = result;
			}
		}
		private int prepare_motor_cmd(int cont_x, int cont_y, int cont_z, int cont_w)
		{
			int PWM_DUTY_MIN = 3999; //65536 * 0.061022
			int PWM_DUTY_IDLE = 6143; //65536 * 0.093741	// 6085
			int PWM_DUTY_MAX = 8172; //65536 * 0.0124697
			int PWM_MIN_MAX_STEP = PWM_DUTY_MAX - PWM_DUTY_MIN;
			double PWM_MULTIFLYER = (double) (PWM_MIN_MAX_STEP) * 0.01;
			double value = 0;;
			int pwm_val = 0;
			int result = 0;

			if(config_mod_int == 0)	{
				value = ((cont_y+50)*PWM_MULTIFLYER) + PWM_DUTY_MIN;
			}
			else if (config_mod_int == 1)	{
				value = ((cont_x+50)*PWM_MULTIFLYER) + PWM_DUTY_MIN;
			}
				pwm_val = (int) value;
				result = (pwm_val & 0xFFFF)<<16;
			if(config_mod_int == 0)	{
				value = ((cont_z+50)*PWM_MULTIFLYER) + PWM_DUTY_MIN;
			}
			else if (config_mod_int == 1)	{
				value = ((cont_w+50)*PWM_MULTIFLYER) + PWM_DUTY_MIN;
			}
				pwm_val = (int) value;

			result |= (pwm_val & 0xFFFF);
			return result;
		}
		public boolean onTouchEvent(MotionEvent event){
			int action = event.getAction();
			int ptrID = event.getPointerId(0);

			pointer_counter = event.getPointerCount();

			if(pointer_counter>MAX_POINTERS) pointer_counter = MAX_POINTERS;

			if(pointer_counter>0 && pointer_counter<=MAX_POINTERS)	{
				ptrID = (action & MotionEvent.ACTION_POINTER_ID_MASK)
						>>MotionEvent.ACTION_POINTER_ID_SHIFT;
				action = action & MotionEvent.ACTION_MASK;

				 //Log.d(LOGTAG, "action"+action+", ptrID="+ptrID+"count="+event.getPointerCount());
				switch(action)	{
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_POINTER_DOWN:
					{
						point[ptrID] = new PointF(event.getX(), event.getY());
						break;
					}

					case MotionEvent.ACTION_MOVE:	{
						for(int i=0;i<pointer_counter;++i){
							int ptrID2 = event.getPointerId(i);
							if(point[ptrID2]!=null)
								point[ptrID2].set(event.getX(i), event.getY(i));
						}

						break;
					}
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:
					{
						point[ptrID]=null;
						break;
					}
				}
			}
			invalidate();
			return true;

		}
/*
		public void onSensorChanged(SensorEvent event) {
			synchronized (this) {
				float var0 = event.values[0];
				float var1 = event.values[1];
				float var2 = event.values[2];

				//Log.d(LOGTAG, "[RYAN] onSensorChanged - type:"+event.sensor.getType());
				switch (event.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					float MAX_ACC = 6;
					int MAX_VAL = 50;
					//Log.d(LOGTAG, "x = " + var0 + ", y = " + var1 +" , z = " + var2);
					key_accel = (int) ((var1/MAX_ACC) * -50);
					if(key_accel > MAX_VAL) key_accel = MAX_VAL;
					if(key_accel < -1*MAX_VAL) key_accel = -1*MAX_VAL;
					//Log.d(LOGTAG, "ACCEL_KEY:" + key_accel);
					if(ex_key_accel!=key_accel)	invalidate();
					break;

				default:
					break;
				}
			}
		}
*/
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			float var0 = event.values[0];
			float var1 = event.values[1];
			float var2 = event.values[2];

			//Log.d(LOGTAG, "[RYAN] onSensorChanged - type:"+event.sensor.getType());
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				float MAX_ACC = 6;
				int MAX_VAL = 50;
				//Log.d(LOGTAG, "x = " + var0 + ", y = " + var1 +" , z = " + var2);
				key_accel = (int) ((var1/MAX_ACC) * -50);
				if(key_accel > MAX_VAL) key_accel = MAX_VAL;
				if(key_accel < -1*MAX_VAL) key_accel = -1*MAX_VAL;
				//Log.d(LOGTAG, "ACCEL_KEY:" + key_accel);
				if(Math.abs(ex_key_accel-key_accel)>1)	{
					mView.invalidate();
					ex_key_accel = key_accel;
				}
				break;

			default:
				break;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
}
