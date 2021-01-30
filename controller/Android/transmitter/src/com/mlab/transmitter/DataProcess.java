package com.mlab.transmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.util.Log;

public class DataProcess {
	public static final String LOGTAG = "SierraWhiskey";
	public static byte RELAYOPT=1;
	public static byte RELAYCHK=2;
	public static byte RELAYSTATE=3;
	public static byte APPQUIT=4;
	public static byte CLOSETCP=5;
	public static byte MAINMENU=6;

	public static byte GET_REMOTE_INFO=7;
	public static byte CHECK_CONNECTION=8;

    static Semaphore mutex = new Semaphore(1);

   protected Socket socket            ;//Socket 占쎄꼍��
         //占쏙옙�껃죰占쎈－
   public boolean State;
 	private byte[] sData=new byte[1024];
 	//private byte[] sData=new byte[1024*10];
 	ReadThread readThread=null;
 	HandleMsg hOptMsg=null;
 	Context mct=null;
	 /**
	 * @param s
	 * @param ctrlHandle
	 */
 	public DataProcess(HandleMsg hmsg,Context context)
 	{
		socket = new Socket();
		hOptMsg=hmsg;
		mct=context;
	}

	public boolean sendDataInt(int data) throws IOException {
		// TODO Auto-generated method stub
		//Log.d(LOGTAG,"sendData");
		OutputStream out=socket.getOutputStream();
		if(out==null) return false;

		//Log.d(LOGTAG,"sendData : "+data);

		out.write(data);
		return true;
	}

	public boolean sendData(byte[] data) throws IOException {
		// TODO Auto-generated method stub
		//Log.d(LOGTAG,"sendData");
		OutputStream out=socket.getOutputStream();
		if(out==null) return false;

		/*
		Log.d(LOGTAG,"sendData : "+data[0]
			+", "+data[1]
			+", "+data[2]
			+", "+data[3]
			+", "+data[4]
			+", "+data[5]
			+", "+data[6]
			+", "+data[7]);*/
	    try {
			mutex.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.write(data);
		mutex.release();
		return true;
	}

	public boolean stopConn() {
		State=false;
		if(readThread==null) return false;
		readThread.abortRead();
		return false;
	}


	public boolean startConn( String  ip,int port) {
		Log.d(LOGTAG,"startConn ip:"+ip+", port:"+port+"isClosed:"+socket.isClosed());

		if(socket.isClosed()) socket=new Socket();
		SocketAddress remoteAddr=new InetSocketAddress(ip,port);

		try {
			Log.d(LOGTAG,"startConn socket.connect.[SocketAddress:"+remoteAddr+"]");
			socket.connect(remoteAddr, 1000);
			Log.d(LOGTAG,"startConn socket.connect done.");
		} catch (IOException e) {
			socket=new Socket();
			Log.d(LOGTAG, e.getMessage());
			return false;
		} /*catch (IllegalArgumentException e) {
			Log.d(LOGTAG, e.getMessage());
		}*/

		Log.d(LOGTAG,"startConn ReadThread.");
		this.readThread=new ReadThread(hOptMsg, sData,socket);
		Log.d(LOGTAG,"startConn start ReadThread.");
		readThread.start();
		Log.d(LOGTAG,"startConn start ReadThread done.");

		State=true;
		return true;
	}
	byte test = -128;
	public byte[] packageCmd(int id,int opt)
	{
		//if(id>5) return null;
		/* HEADER : BR, END : T" Total 3+5(data) BYTE */
		byte[] cmd = new byte[]{0x62,0x72,0x0,0x0,0x0,0x0,0x0,0x0,0x0};

		cmd[2] = (byte) ((id & 0xFF));

		cmd[3] = (byte) ((opt >> 24));
		cmd[4] = (byte) (((opt >> 16) & 0xFF));
		cmd[5] = (byte) (((opt >> 8) & 0xFF));
		cmd[6] = (byte) ((opt) & 0xFF);

		cmd[7] = (byte) 0xED;
		cmd[8] = (byte) 0xEE;
/*
		for(int x=0;x<6;x++)
		{
			cmd[x] = test;
			if(test<254) test++;
			else test=0;
			//cmd[x]-= 128;
		}
*/
		return cmd;
	}

	int test1=0;
	public void sendrelayCmd(int id,int opt)
	{
		byte[] cmd=packageCmd(id, opt);
		/*
		Log.d(LOGTAG,"sendrelayCmd id:"+id+", opt:"+opt+", cmd:"+cmd[0]
			+", "+cmd[1]
			+", "+cmd[2]
			+", "+cmd[3]
			+", "+cmd[4]
			+", "+cmd[5]
			+", "+cmd[6]
			+", "+cmd[7]
			+", "+cmd[8]);*/
		//if(cmd==null) return;
		if((readThread==null)||(readThread.state==false))
		{
			//RelayCtrlActivity.showMessage(mct.getString(R.string.msg5));
			hOptMsg.stateCheck(0);
			return;
		}
		try {
			sendData(cmd);
			//sendDataInt(test1++);
			if(id!=5) hOptMsg.stateCheck(2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			//RelayCtrlActivity.showMessage(mct.getString(R.string.msg4));
		//	hOptMsg.sendEmptyMessage(DataProcess.CLOSETCP);
		}
	}
}
