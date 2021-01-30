package com.mlab.transmitter;

import java.io.IOException;
import java.net.Socket;

import android.os.Message;
import android.util.Log;

public class ReadThread extends Thread {
	boolean state;
	HandleMsg hOptMsg;
	byte[] sData;
	Socket socket;
	public static final String LOGTAG = "SierraWhiskey";
	public ReadThread(HandleMsg hmsg,byte[] sData,Socket socket)
	{
		hOptMsg=hmsg;
		this.sData=sData;
		this.socket=socket;
	}
	/*
	 * @see java.lang.Thread#run()
	 * 瀛욜쮮�ζ뵸訝삣쑵��	 */

	public void run()
	 {
		int rlRead;
		state=true;
		try
		{
			while(state)
			{
				//Log.d(LOGTAG,"run");
				rlRead=socket.getInputStream().read(sData);//野방뼶���瓦붷썮-1
				//Log.d(LOGTAG,"rlRead="+rlRead+"String:"+sData.toString());
				if(rlRead>0)
				{
					//RelayCtrlActivity.showMessage(sData.toString());
					unpackageCmd(sData,rlRead);

				}
				else
				{
				    	state=false;
					Log.d(LOGTAG,"run CLOSETCP");
				    	hOptMsg.sendEmptyMessage(DataProcess.CLOSETCP);
					break;
 				}
			}
	    }
	    catch (Exception e) {
	    	Log.v(LOGTAG, e.getMessage());
	    	state=false;
	    	hOptMsg.sendEmptyMessage(DataProcess.CLOSETCP);
	    }
	 }



	public void abortRead()
	{
		if(socket==null) return;
		try
		{
		   socket.shutdownInput();
		   socket.shutdownOutput();
		   socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		state = false;
	}

	public void unpackageCmd(byte[] cmd,int len)
	{
		boolean result;
		int payload[]={0,0,0};
		//Log.d(LOGTAG,"unpackageCmd in:"+len);
		result = false;
		for(int x=0; x<len-10 ; x++)
		{
			if(cmd[x] == (byte) 0x62)	{
				if((cmd[x+1] == (byte) 0x72) &&
				(cmd[x+9] == (byte) 0xED) && (cmd[x+10] == (byte) 0xEE))	{
					payload[0] = ((byte) cmd[x+3] & 0xFF)<<8 | ((byte) cmd[x+4] & 0xFF);
					payload[1] = ((byte) cmd[x+5] & 0xFF)<<8 | ((byte) cmd[x+6] & 0xFF);
					payload[2] = ((byte) cmd[x+7] & 0xFF)<<8 | ((byte) cmd[x+8] & 0xFF);
					//Log.d(LOGTAG,"unpackageCmd payload ={"+(cmd[2] & 0xFF)+", "+(cmd[3] & 0xFF)+", "+(cmd[4] & 0xFF)+", "+(cmd[5] & 0xFF)+", "+(cmd[6] & 0xFF)+"}");
					//Log.d(LOGTAG,"unpackageCmd payload ={"+payload[0]+", "+payload[1]+"}");

					if(hOptMsg==null) return;
					Message msg=new Message();
					msg.what=DataProcess.GET_REMOTE_INFO;
					msg.arg1= (cmd[x+2] & 0xFF) | (payload[2]<<16);
					msg.arg2= (payload[0]<<16) | (payload[1] & 0xFFFF);
					hOptMsg.sendMessage(msg);
					result = true;
				}
			}
		}
		if(!result)
			Log.d(LOGTAG,"unpackageCmd: No command was found in "+len+"bytes buffer.");
	}
	public void unpackageCmd_org(byte[] cmd,int len)
	{

		/*int size=len;
		if(size<=0) return;

		int i;

		boolean cmdStart=false;
		int cmdInx = 0;
		byte sum=0;
		for(i=0;i<size;i++)
		{
			if(cmdStart==false)
			{//22 01 00 01 01 01 02 23
				if(cmd[i]==0x22)
				{
					cmdStart=true;
					cmdInx  =0;
					sum=0x22;
				}
			}
			else
			{
				cmdInx++;
				switch(cmdInx)
				{
				   case 1:
					   if(cmd[i]!=0x01) cmdStart=false;
					   sum+=cmd[i];
					break;
				   case 2:
					   if((cmd[i]&0xFE)!=0x00) cmdStart=false;
					   sum+=cmd[i];
					break;
				   case 3:
				   case 4:
				   case 5:
				   case 6:
					   sum+=cmd[i];
					break;
				   case 7:
					   if(cmd[i]==sum)
					   {
							if(hOptMsg==null) return;
							Message msg=new Message();
							msg.what=DataProcess.RELAYSTATE;
							msg.arg1=cmd[i-1];
							msg.arg1=(msg.arg1<<8)|cmd[i-2];
							msg.arg1=(msg.arg1<<8)|cmd[i-3];
							msg.arg1=(msg.arg1<<8)|cmd[i-4];
							hOptMsg.sendMessage(msg);
					   }
					   cmdStart=false;
						break;
				}
			}
		}*/
	}
}










