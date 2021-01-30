package com.mlab.transmitter;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UiProcess {

   public static final String LOGTAG = "SierraWhiskey";
   public static byte MSGOPTON  =2;
   public static byte MSGOPTOFF =1;
   public static byte MSGOPTNONE=0;

   public static byte MSGOPTRELAll=0;
   public static byte MSGOPTREL1=1;
   public static byte MSGOPTREL2=2;
   public static byte MSGOPTREL3=3;
   public static byte MSGOPTREL4=4;

   HandleMsg hUiMsg=null;
   Context mct=null;
   DataProcess dataProcess=null;

	private ImageButton bnConnect;
	private EditText etIp,etPort;
	private TextView tv1,tv2,tv3,tv4;
	private ImageView im1,im2,im3,im4;
	QPopupWindow createConfigWindow;

	public String nameRel1,nameRel2,nameRel3,nameRel4;

	SoundPool snd = new SoundPool(10, AudioManager.STREAM_SYSTEM,5);
	int hitOkSfx;
	private Thread thread_connect = null;

	ProgressDialog pdialog ;

	public UiProcess(Context context,HandleMsg hMsg,DataProcess dProcess) {
		super();
		Button bnRealyOn,bnRelayOff;


		hUiMsg=hMsg;
		mct=context;
		dataProcess=dProcess;



		Log.d(LOGTAG,"UiProcess");

		 loadConfigure();
		 updateName();

		 setRelayState(0);

		 loadIpPort();

 		thread_connect();
 		thread_connect.start();

		 Log.d(LOGTAG,"UiProcess done");

   }



	private void thread_connect() {
		thread_connect = new Thread() {

				public void run(){
					try{
						{
							Log.d(LOGTAG,"thread_connect");
							 //snd.play(hitOkSfx, (float)0.5, (float)0.5, 0, 0, 1);
							// TODO Auto-generated method stub
							 if(isEditEnable())
							 {
								 //Log.d(LOGTAG,"setOnClickListener 1");
								String sip	  ="10.10.100.254";
								String sport  = "8899";
								Log.d(LOGTAG,"setOnClickListener sip:"+sip+", sport:"+sport);

								Pattern pa=Pattern.compile("^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$");
								Matcher ma=pa.matcher(sip);
							  if(ma.matches()==false)
							   {
								  //Log.d(LOGTAG,"setOnClickListener 2");
								  //RelayCtrlActivity.showMessage(mct.getString(R.string.msg6));
								  return;
							   }

							  //Log.d(LOGTAG,"setOnClickListener 3");
								 int port=0;
									try {
										port=Integer.parseInt(sport);
									} catch (Exception e) {
										 //RelayCtrlActivity.showMessage(mct.getString(R.string.msg7));
										return ;
									}

									//Log.d(LOGTAG,"setOnClickListener 4");
								 if(dataProcess.startConn(sip, port))
								 {
									 //Log.d(LOGTAG,"setOnClickListener 5");
									 //RelayCtrlActivity.showMessage(mct.getString(R.string.msg2));
									 //bnConnect.setImageResource( R.drawable.true1);

									 hUiMsg.stateCheck(1);
									 setEditEnable(false);
								 }
								 else
								 {
								 	Log.d(LOGTAG,"startConn failed!");
									 //RelayCtrlActivity.showMessage(mct.getString(R.string.msg1));
								 }
							 }
							 else
							 {
								 stopConn();
							 }
						}


					}catch(Exception e){
						Log.d(LOGTAG, e.getMessage());
					}
				}
			};
		}


	public void stopConn( )
	{
		 Log.d(LOGTAG,"stopConn");

		 dataProcess.stopConn();
		 hUiMsg.stateCheck(0);

		 //RelayCtrlActivity.showMessage(mct.getString(R.string.msg3));
		 setEditEnable(true);
		 setRelayState(0);
	}

	public void genStick(int key, int value)
	{
		if(hUiMsg==null) return;

		Message msg=new Message();
		msg.what=DataProcess.RELAYOPT;
		msg.arg1=key;
		msg.arg2=value;
		hUiMsg.sendMessage(msg);

	}

	public void setEditEnable(boolean en)
	{
		Log.d(LOGTAG,"setEditEnable:"+en);
		/*
		etIp.setEnabled(en);
		etPort.setEnabled(en);
		if(en==false)
		{
			etIp.setInputType(InputType.TYPE_NULL);
			etPort.setInputType(InputType.TYPE_NULL);
		}
		else
		{
			etIp.setInputType(InputType.TYPE_CLASS_TEXT);
			etPort.setInputType(InputType.TYPE_CLASS_TEXT);
		}*/
	}

	public boolean isEditEnable()
	{
		return true;//etIp.isEnabled();
	}

	class onRelayButtonClick implements View.OnClickListener
	{
		private int operate_l,relayId_l;

		public onRelayButtonClick(byte relayId,byte operate)
		{
			relayId_l=relayId;
			operate_l=operate;
		}
		Button bnActivity,bn;

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Log.d(LOGTAG,"onClick - relayId_l:"+relayId_l+", operate_l:"+operate_l);
			snd.play(hitOkSfx, (float)0.5, (float)0.5, 0, 0, 1);
			if(hUiMsg==null) return;

			Message msg=new Message();
			msg.what=DataProcess.RELAYOPT;
			msg.arg1=relayId_l;
			msg.arg2=operate_l;
			hUiMsg.sendMessage(msg);
		}
	}



	private byte[] state_l=new byte[]{(byte)0xFF,(byte)0xff,(byte)0xFF,(byte)0xff};

	public void setRelayState(int state)
	{
		Log.d(LOGTAG,"setRelayState:"+state);
		byte sat=(byte)(state&0xff);
 		if(state_l[0]!=sat)
 		{
 			state_l[0]=sat;
 			setRelayOnoff(tv1,im1,sat);
 		}



		state=state>>8;
		sat=(byte)(state&0xff);
 		if(state_l[1]!=sat)
 		{
 			state_l[1]=sat;
 			setRelayOnoff(tv2,im2,sat);
 		}


		state=state>>8;
		sat=(byte)(state&0xff);
 		if(state_l[2]!=sat)
 		{
 			state_l[2]=sat;
 			setRelayOnoff(tv3,im3,sat);
 		}

		state=state>>8;
		sat=(byte)(state&0xff);
 		if(state_l[3]!=sat)
 		{
 			state_l[3]=sat;
 			setRelayOnoff(tv4,im4,sat);
 		}

	}


	public void setRelayOnoff(TextView tv,ImageView im,byte state)
	{//占쏙옙�뽭�琉꾩삕�좑옙
		Log.d(LOGTAG,"setRelayOnoff:"+state);
	}

	private EditText etRelay1,etRelay2,etRelay3,etRelay4;
	     public void loadConfigure()
	     {
	  	    SharedPreferences uiState   = mct.getSharedPreferences("system", mct.MODE_PRIVATE);
	     }

	     public void saveConfigure()
	     {
	  	    SharedPreferences uiState   = mct.getSharedPreferences("system", mct.MODE_PRIVATE);
	  		Editor et=uiState.edit();
	  		et.commit();
	     }

	     public void updateName()
	     {
	     }


	     public void saveIpPort()
	     {
	  	    SharedPreferences uiState   = mct.getSharedPreferences("system", mct.MODE_PRIVATE);
	  		Editor et=uiState.edit();
			Log.d(LOGTAG,"saveIpPort ip:"+"10.10.100.254"+", port:"+"8899");

	  		et.putString("ip","10.10.100.254");
	  		et.putString("port","8899");
	  		et.commit();
	     }

	     public void loadIpPort()
	     {
	     	Log.d(LOGTAG,"loadIpPort 0");
	  	    SharedPreferences uiState   = mct.getSharedPreferences("system", mct.MODE_PRIVATE);
	     	Log.d(LOGTAG,"loadIpPort 1");

	  	    //etIp.setText(uiState.getString("ip","192.168.1.100" ));
	  	    //etIp.setText(uiState.getString("ip","10.10.100.254" ));
	     	Log.d(LOGTAG,"loadIpPort 2");

	  	    //etPort.setText(uiState.getString("port", "8899"));
			Log.d(LOGTAG,"loadIpPort ip:"+uiState.getString("ip","10.10.100.254" )+", port:"+uiState.getString("port", "8899"));

	     }


}
