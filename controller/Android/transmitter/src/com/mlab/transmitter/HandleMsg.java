package com.mlab.transmitter;

import android.os.Handler;

public class HandleMsg extends Handler{
   
	public boolean ckeck; 
	public void stateCheck(int opt)
	{
		if(opt==0)
		{		
			ckeck=false;
			removeMessages(DataProcess.RELAYCHK);
		}
		else if(opt==1)
		{		
			ckeck=true;	
			sendEmptyMessageDelayed(DataProcess.RELAYCHK, 500);
		}
		else  
		{		
			this.removeMessages(DataProcess.RELAYCHK);
			if(ckeck) 	this.sendEmptyMessageDelayed(DataProcess.RELAYCHK, 500);
		}		
	}
}
