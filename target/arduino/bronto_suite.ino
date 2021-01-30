#include <PWM.h>

#include <SoftwareSerial.h>

int WIFI = 0;
int BT = 1;
int phyType = WIFI;

SoftwareSerial mySerial(2, 3); // RX, TX
const int LED_R = 7;
const int LED_G = 6;
const int LED_B = 5;
const int ULTRA_TRIG = 11;
const int ULTRA_ECHO = 12;

/* Motor Configuration */
const int MOTOR_ACCEL = 9;
const int MOTOR_STEERING = 10;
const int PWM_FREQ = 62.427818;
const int PWM_DUTY_MIN = 3999; //65536 * 0.061022
const int PWM_DUTY_IDLE = 6143; //65536 * 0.093741
const int PWM_DUTY_MAX = 8172; //65536 * 0.0.124697
const int PWM_MIN_MAX_STEP = PWM_DUTY_MAX - PWM_DUTY_MIN;
int mSteeringInfo = PWM_DUTY_IDLE;
int mAccelInfo = PWM_DUTY_IDLE;

int mStatus = 0;
int gMainStatus = 0;
int MAIN_WIFI_CONNECT_MASK = 1;
int MAIN_WIFI_COMMAND_MASK = 1<<1;
int WIFI_TIME_OUT_MS = 2000;
int FRAME_ID_COMMAND = 0xF0;
int FRAME_ID_PING = 0x5;

int LED_SYNC_MS = 1000;
int LED_SYNC_ONESHOT_MS = 50;
int LED_oneshot_enable = 0;
int LED_status = 0;
int LED_R_MASK = 1;
int LED_G_MASK = 1<<1;
int LED_B_MASK = 1<<2;

/* Ultrasound Configuration */
int USE_ULTRASOUND_SENSOR = 1;
int ULTRA_distance = 0;
int ULTRA_READ_PERIOD_MS = 100;

/* Driving Algorithm */
int MIN_DISTANCE = 15;  // cm

int inByte = 0;
void setup()  
{
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);
  pinMode(MOTOR_ACCEL, OUTPUT);
  pinMode(MOTOR_STEERING, OUTPUT);
  pinMode(ULTRA_TRIG, OUTPUT);
  pinMode(ULTRA_ECHO, INPUT);

  InitTimersSafe();
  SetPinFrequency(MOTOR_ACCEL, PWM_FREQ);
  SetPinFrequency(MOTOR_STEERING, PWM_FREQ);
  MotorControl(PWM_DUTY_IDLE, PWM_DUTY_IDLE);
 
  digitalWrite(LED_R, LOW);
  digitalWrite(LED_G, LOW);
  digitalWrite(LED_B, LOW);
  // Open serial communications and wait for port to open:
  if(phyType == WIFI)  {
    Serial.begin(115200);
  }
  else	{
    Serial.begin(9600);
  }
  while (!Serial) {
    ; // wait for serial port to connect. Needed for Leonardo only
  }

  Serial.println("Brontosaurus Suite (WIFI version)");

  // set the data rate for the SoftwareSerial port
  //mySerial.begin(115200);
  mySerial.begin(57600);
  
  mStatus = 0;
  gMainStatus = 0;
}

int check_frame = 0;
int frame_data[7]={0,0,0,0,0,0,0};
int frame_ready = 0;
int frame_id = 0;
int frame_payload[2] = {0,0};
int ex_connect_sync_ms = 0;
int ex_ultra_sync_ms = 0;
int ex_LED_sync_ms = 0;
int ex_LED_oneshot_sync_ms = 0;


void loop() // run over and over
{
  int cur_time_ms = millis();
  if((ex_connect_sync_ms != 0) &&
      (cur_time_ms - ex_connect_sync_ms < WIFI_TIME_OUT_MS))  {  // Connected
    if(gMainStatus & MAIN_WIFI_CONNECT_MASK == 0)
    Serial.print("Connected\r\n");
    gMainStatus |= MAIN_WIFI_CONNECT_MASK;

  }
  else {  // Disconnected
    if(gMainStatus & MAIN_WIFI_CONNECT_MASK)
        Serial.print("Disconnected\r\n");
    gMainStatus &= !MAIN_WIFI_CONNECT_MASK;
  }
  if (mySerial.available()>0)  {
    inByte = mySerial.read();
    switch(check_frame)
    {
      case 0:
        if(inByte == 0x62)  check_frame++;
      break;
      case 1:
        if(inByte == 0x72)  check_frame++;
        else check_frame = 0;
        //Serial.print("state.1\r\n");
      break;
      case 2:
        frame_data[frame_ready] = inByte;
        frame_ready++;
        if(frame_ready>=7) check_frame++;
        //Serial.print("state.2\r\n");
      break;
      default:
      break;
    }
    if(check_frame==3)  {
      if(frame_data[5] == 0xED && frame_data[6] == 0xEE)  {
        gMainStatus |= MAIN_WIFI_COMMAND_MASK;
        
        frame_id = frame_data[0];
        frame_payload[0] = frame_data[2];       
        frame_payload[0] |= frame_data[1]<<8;
        frame_payload[1] = frame_data[4];
        frame_payload[1] |= frame_data[3]<<8; 
        
        /*
        Serial.print("Command Available - CMD:");
        Serial.print(frame_data[0], HEX);
        Serial.print(", Payload[CH0]=");
        Serial.print(frame_payload[0], DEC);
        Serial.print(", [CH1]=");
        Serial.print(frame_payload[1], DEC);
        */
        //Serial.print(", WIFI_Timeout=");
        //Serial.print(cur_time_ms - ex_connect_sync_ms, DEC);
        
        Serial.print("\r\n");
        
        ex_connect_sync_ms = cur_time_ms;
      }
      else {
        /*
        Serial.print("Wrong command :");
        Serial.print(frame_data[6], HEX);
        Serial.print(", ");
        Serial.print(frame_data[7], HEX);
        Serial.print("\r\n");
        */
      }
      frame_ready = check_frame = 0;
    }
    
    #if 0
    static int flag=0;
    Serial.print(inByte, HEX);
    if(inByte == 0xEE && flag ==1)  {
      flag =0;
      Serial.print("\r\n");
    }
    else     Serial.print(", ");
    
    if(inByte == 0xED)  flag =1;
    else flag=0;
    #endif

    if(phyType == BT)  {
      inByte = toupper(inByte);
      //state = (inByte >= 'A') ? inByte - 'A' +10 : inByte - '0';
    }
  }
/* Update Distance info */
#if 1
if(USE_ULTRASOUND_SENSOR) {
  if(cur_time_ms - ex_ultra_sync_ms > ULTRA_READ_PERIOD_MS)
  {
    digitalWrite(ULTRA_TRIG,HIGH);
    delayMicroseconds(10);
    digitalWrite(ULTRA_TRIG,LOW);
    ULTRA_distance = pulseIn(ULTRA_ECHO, HIGH)/29/2;
    ex_ultra_sync_ms = cur_time_ms;
  }
}
/* Update Motor info */
    if(gMainStatus & MAIN_WIFI_CONNECT_MASK)  {  // WIFI is connected.
         if (frame_id == FRAME_ID_COMMAND)  {
            mAccelInfo = frame_payload[0];
            mSteeringInfo = frame_payload[1];
            MotorControl(mSteeringInfo, mAccelInfo);
         }
    }
    else {
          MotorControl(PWM_DUTY_IDLE, PWM_DUTY_IDLE);
          mSteeringInfo = mAccelInfo = PWM_DUTY_IDLE;
    }
#endif  
/* Update LED info */
  if(gMainStatus & MAIN_WIFI_CONNECT_MASK)  {  // WIFI connected.
    if(LED_status & LED_R_MASK)  {
      digitalWrite(LED_R, LOW);
      LED_status &= !LED_R_MASK;
    }    
    if(cur_time_ms - ex_LED_sync_ms > LED_SYNC_MS)  {
      if(LED_status & LED_B_MASK) {
        //digitalWrite(LED_B, LOW);
        LED_status &= !LED_B_MASK;
      }
      else {
        //digitalWrite(LED_B, HIGH);
        LED_status |= LED_B_MASK;
      }
      ex_LED_sync_ms = cur_time_ms;
    }
  }
  else {
    if(LED_status & LED_B_MASK)  {
      digitalWrite(LED_B, LOW);
      LED_status &= !LED_B_MASK;
    }
    if(cur_time_ms - ex_LED_sync_ms > LED_SYNC_MS)  {
      if(LED_status & LED_R_MASK) {
        digitalWrite(LED_R, LOW);
        LED_status &= !LED_R_MASK;
      }
      else {
        digitalWrite(LED_R, HIGH);
        LED_status |= LED_R_MASK;
      }
      ex_LED_sync_ms = cur_time_ms;
    }
  }

  if(gMainStatus & MAIN_WIFI_COMMAND_MASK)  {  // WIFI command.
    digitalWrite(LED_G, HIGH);
    LED_status |= LED_G_MASK;
    gMainStatus &= !MAIN_WIFI_COMMAND_MASK;
    LED_oneshot_enable = 1;
    ex_LED_oneshot_sync_ms = cur_time_ms;
  }
  if(LED_oneshot_enable)  {
    if (cur_time_ms - ex_LED_oneshot_sync_ms > LED_SYNC_ONESHOT_MS)  {
      digitalWrite(LED_G, LOW);
      LED_status &= !LED_G_MASK;
      LED_oneshot_enable = 0;
    }    
  }

  /* Print message */
  DebugMessage(0);
}

void MotorControl(int steering, int accel)
{
  if(steering > PWM_DUTY_MAX || steering < PWM_DUTY_MIN)  {
    pwmWriteHR(MOTOR_STEERING, PWM_DUTY_IDLE);
    Serial.print("Motor - Out of Range - Steering\r\n");
    return;
  }

  if(accel > PWM_DUTY_MAX || accel < PWM_DUTY_MIN)  {
    pwmWriteHR(MOTOR_ACCEL, PWM_DUTY_IDLE);
    Serial.print("Motor - Out of Range - Accelator\r\n");
    return;
  }

  pwmWriteHR(MOTOR_STEERING, steering);  
  pwmWriteHR(MOTOR_ACCEL, accel);
}

void DrivingControl(void)
{
  if(ULTRA_distance < MIN_DISTANCE)  {
    if (mAccelInfo > PWM_DUTY_IDLE)
        mAccelInfo = PWM_DUTY_IDLE;
  }
}

void DebugMessage(int enable)
{
  if(enable)  {
    Serial.print("Global Status:");
    Serial.print(gMainStatus, DEC);
    Serial.print(", Distance:");
    Serial.print(ULTRA_distance, DEC);
    Serial.print("cm, SteeringInfo:");
    Serial.print(mSteeringInfo, DEC);
    Serial.print(", AccelInfo:");
    Serial.print(mAccelInfo, DEC);
    Serial.print("\r\n");
  }
}
