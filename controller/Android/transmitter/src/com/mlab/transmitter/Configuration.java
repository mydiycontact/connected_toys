package com.mlab.transmitter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class Configuration extends Activity implements android.widget.RadioGroup.OnCheckedChangeListener {
    public static final String LOGTAG = "SierraWhiskey";
	private SeekBar mc;
	private CheckBox cb_accel;
	private TextView text_motor;
	
	RadioGroup radioGroup;
	private RadioButton option1;
	private RadioButton option2;
	private RadioButton option3;

	int m_motorstrength;
	boolean m_use_accel;
	int m_config_mod;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.configuration);
	// Update Param
	SharedPreferences prefs = getSharedPreferences("Configuration", MODE_PRIVATE);
	m_motorstrength = prefs.getInt("MotorStrengthInt", 100);
	m_use_accel = prefs.getBoolean( "Enable_accel", false);
	m_config_mod = prefs.getInt( "Mode Config", 0);
		
	mc = (SeekBar) findViewById(R.id.motorstrength);
	mc.setProgress(m_motorstrength);
	text_motor = (TextView) findViewById(R.id.info_motorstrength_val);
	text_motor.setText(": "+String.valueOf(m_motorstrength)+"%");
	cb_accel = (CheckBox) findViewById(R.id.use_accel);
	cb_accel.setChecked(m_use_accel);

    radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
	option1 = (RadioButton) findViewById(R.id.mode_option1);
	option2 = (RadioButton) findViewById(R.id.mode_option2);
	option3 = (RadioButton) findViewById(R.id.mode_option3);
	radioGroup.setOnCheckedChangeListener(this);

	switch(m_config_mod)	{
		case 0:
			option1.setChecked(true);
			break;
		case 1:
			option2.setChecked(true);
			break;
		case 2:
			option3.setChecked(true);
			break;
		default:
			break;
	}

	
    mc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
	             // TODO Auto-generated method stub
	    }
	   
	    @Override
	    public void onStartTrackingTouch(SeekBar seekBar) {
	         // TODO Auto-generated method stub
	        
	    }
	   
	    @Override
	    public void onProgressChanged(SeekBar seekBar, int progress,
	               boolean fromUser) {
	         // TODO Auto-generated method stub
	    m_motorstrength = progress;
	    text_motor.setText(": "+String.valueOf(m_motorstrength)+"%");
	    
  		SharedPreferences prefs = getSharedPreferences("Configuration", MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putInt( "MotorStrengthInt" , m_motorstrength);
		ed.commit();        
	    }
    });

    cb_accel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override 
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          if (buttonView.isChecked())	m_use_accel = true;
          else	m_use_accel = false;

          SharedPreferences prefs = getSharedPreferences("Configuration", MODE_PRIVATE);
    		Editor ed = prefs.edit();
    		ed.putBoolean( "Enable_accel" , m_use_accel);
    		ed.commit();        

        }
    });

    
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// TODO Auto-generated method stub
		if(radioGroup.getCheckedRadioButtonId() == R.id.mode_option1)	{
			m_config_mod = 0;
			//Toast.makeText(this, "2CH MONSTER TRUCK", Toast.LENGTH_SHORT).show();
		}
		else if(radioGroup.getCheckedRadioButtonId() == R.id.mode_option2)	{
			m_config_mod = 1;
			//Toast.makeText(this, "2CH DLG", Toast.LENGTH_SHORT).show();
		}
		else if(radioGroup.getCheckedRadioButtonId() == R.id.mode_option3)	{
			//Toast.makeText(this, "3CH SAIL BOAT", Toast.LENGTH_SHORT).show();
			m_config_mod = 2;			
		}
		else
			return;
  		SharedPreferences prefs = getSharedPreferences("Configuration", MODE_PRIVATE);
		Editor ed = prefs.edit();
		ed.putInt( "Mode Config" , m_config_mod);
		ed.commit();    
	}
	
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
	@Override
	public void onStop() {
		super.onStop();
		Log.e(LOGTAG, "-- ON STOP --");
	}

}
