#include "sys.h"
#include "delay.h"
#include "string.h"
#include "main.h"
#include "usart.h" 
#include "usart2.h" 
#include "usart3.h" 
#include "led.h" 		 	 
#include "lcd.h"  
#include "key.h"     
#include "usmart.h" 
#include "malloc.h"
#include "sdio_sdcard.h"  
#include "w25qxx.h"    
#include "ff.h"  
#include "exfuns.h"   
#include "text.h"
#include "dht11.h"
#include "math.h"
#include "adc.h"
#include "lsens.h"
#include "mqtt.h"
#include "wifi.h"
#include "pwm.h"
#include "motor.h"
#include "timer3.h"
#include "HX711.h"
#include "beep.h"
#include "Servo.h"
#include "soil_humi.h"
#include "ph.h"
#include "bump.h"

//WIFI配置
#define WIFI_NAME	            "your_wifi_name"       //wifi名称 
#define WIFI_PASSWD           "your_wifi_password"         //wifi密码
#define MQTT_LEN  		580  //定义最大接收字节数 600
//#define MQTT_BROKERADDRESS		"your_broker_address"      //华为wifi地址
//#define MQTT_BROKERADDRESS		"192.168.43.1" 			//手机wifi地址
//阿里云服务器的登陆配置
#define MQTT_BROKERADDRESS		"your_broker_address"//"your_broker_address"     //wifi地址
//来自开发配置 终端节点MQTT选项中的接入公网
#define MQTT_CLIENTID 			"your_device_id"
#define MQTT_USARNAME 			"your_device_username"
#define MQTT_PASSWD 			"your_device_secret"

#define	MQTT_PUBLISH_TOPIC 		"$oc/devices/your_device_id/sys/properties/report"  //锟斤拷头锟侥硷拷锟斤拷锟斤拷锟接讹拷锟侥和凤拷锟斤拷锟侥伙拷锟斤拷
#define MQTT_COMMANDRESP_TOPIC	"$oc/devices/your_device_id/sys/commands/response/request_id="
#define MQTT_SETPROPERTY_TOPIC "$oc/devices//your_device_id/sys/properties/set/response/request_id="


#define		JSON_Tree_Format	"{ \n "					\
								"\"services\": [{ \n"		\
									"\"service_id\": \"smartlan\", \n"	\
									"\"properties\": { \n"	\
									    "\"Temp\":  \"%s\", \n"	\
											"\"Humi\":  \"%s\", \n"	\
									    "\"Lumi\":  \"%s\", \n"	\
											"\"Fengd\":  \"%s\", \n"	\
											"\"LightSt\":  \"%s\", \n"	\
											"\"AIWarning\":  \"%s\", \n"	\
											"\"DangGuangBan\":  \"%s\", \n"		\
											"\"pH\":  \"%s\", \n"			\
											"\"Bump\":  \"%s\", \n"			\
											"\"Soil_Humi\":  \"%s\", \n"			\
											"\"State\":  \"%s\" \n"	\
											"}, \n"	\
									"\"event_time\": \"\" \n"	\
									"} \n"	\
									"] \n"	\
								"}\n"
								
#define  RESPONSE_CMD "{\"result_code\": 0,\"response_name\": \"COMMAND_RESPONSE\",\"paras\": {\"result\": \"success\"}}"

#define ROUND_TO_UINT16(x)   ((uint16_t)(x)+0.5)>(x)? ((uint16_t)(x)):((uint16_t)(x)+1) //锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷锟诫，转锟斤拷为uint16_t
				
static char tempstr[10] = {0}; //存放温度数据 字符串
static char humitystr[10] = {0}; //存放温度数据 字符串
static char lumistr[10] = {0}; //存放亮度数据 字符串
static char fenDegree[5] = {0};   //风量数据字符串
static char lightstr[10] = {0};   //教室大灯风量数据字符串
static char AIWarningstr[5] = {0};   //AI赋能信息属性
static char Doorstr[10] = {0};  //挡光板数据字符串
static char Bump[10] = {0};  
static char State[10] = {0}; 
static char SoilHumi[10] = {0}; 
static char PH[10] = {0}; 

extern uint8_t usart_su_TXpacket[14];//hex数据包接收数组

char mqtt_message[600];
void MQTT_Init(void);
void Do_command_response(void);

u8 txbuf[MQTT_LEN]={0};
u8 status=0;
u16 Uart3_index=0;


/************************************************
 ALIENTEK精英STM32开发板实验37
 汉字显示 实验 
 技术支持：www.openedv.com
 淘宝店铺：http://eboard.taobao.com 
 关注微信公众平台微信号："正点原子"，免费获取STM32资料。
 广州市星翼电子科技有限公司  
 作者：正点原子 @ALIENTEK
************************************************/


u8 adcx=0;
u8 lightState =0;//灯状态 
u8 aiwarning =0;//AI赋能状态登记
u8 Doorstate=0;
u8 angle=0;
u8 fengspeeddang=0;
u8 state =0	;
u8 bumpstate =0;
u8 soilhumi =0;


uint8_t humiH,humiL,tempH,tempL, phH,phL;	
u8 Serial0_RxFlag=0,pingFlag=0;


char USART3_RX_BUF_BK[USART3_RXBUFF_SIZE]; //定义一个数组，用于保存串口3接收到的数据备份，单独处理
char USART3_RX_BUF[USART3_RXBUFF_SIZE];
char A_JSON_Tree[420] = {0};	// 存放JSON树'

// 创建JSON树
//===================================================================================================
void  Setup_JSON_Tree_JX(void)
{

	printf(" into setup json\n");
	// 赋值JSON树【赋值JSON_Tree_Format字符串中的格式字符】
	//--------------------------------------------------------------------------------------------	
	//sprintf(A_JSON_Tree, JSON_Tree_Format,light_num,distant,humitystr,tempstr,LampSt,CondSt);
	memset(A_JSON_Tree,0,420);
	sprintf(A_JSON_Tree, JSON_Tree_Format,tempstr,humitystr,lumistr,fenDegree,lightstr,AIWarningstr,Doorstr,PH,Bump,SoilHumi,State);

	printf("\r\n-------------------- create JSON tree -------------------\r\n");

	printf("%s",A_JSON_Tree);		// 串口打印JSON树

	printf("\r\n--------------------create JSON tree  -------------------\r\n");


}

void init_allStr(void)
{
	memset(A_JSON_Tree,0,420);
	memset(tempstr,0,10);
	memset(humitystr,0,10);
	memset(lumistr,0,10);
	memset(fenDegree,0,5);
	memset(lightstr,0,10);
	memset(Doorstr,0,10);
	memset(Bump,0,10);
	memset(AIWarningstr,0,10);
	memset(State,0,10);
	memset(SoilHumi,0,10);
	memset(PH,0,10);
	

}
/*******************************
	函数名：获取传感器数据     *
	参  数：无                 *
	返回值：无                 *
********************************/
void GetSensorData(void)
{	
			init_allStr();
			DHT11_Read_Data(&humiH,&humiL,&tempH,&tempL);
			sprintf(humitystr,"%d.%d",humiH,humiL);
			sprintf(tempstr,"%d.%d",tempH,tempL);
	
			adcx=Lsens_Get_Val();			
			sprintf(lumistr,"%d",adcx);
			
			//co2Value=AirSensor_GetECO2();
			//sprintf(co2str,"%d",co2Value);
	
			soilhumi=Soil_Hum_Get_Val();
			sprintf(SoilHumi, "%d", soilhumi);
	
			PH_Read_Data(&phH, &phL);
			sprintf(PH,"%d.%d",phH,phL);
	
	
			LCD_ShowNum(60, 80, tempH, 5, 16);
			Show_Str(110, 80, 64, 16, ".", 16, 0); 
			LCD_ShowNum(120, 80, tempL, 2, 16);
			Show_Str(150, 80, 64, 16, "C", 16, 0);

			LCD_ShowNum(60, 100, humiH, 5, 16);
			Show_Str(110, 100, 64, 16, ".", 16, 0);
			LCD_ShowNum(120, 100, humiL, 2, 16);
			Show_Str(150, 100, 64, 16, "%", 16, 0);

			LCD_ShowxNum(100, 120, adcx, 3, 16, 0);
			Show_Str(150, 120, 64, 16, "nits", 16, 0);
			
			
			LCD_ShowxNum(100, 140, soilhumi, 3, 16, 0);
			Show_Str(150, 140, 64, 16, "%", 16, 0);
			
			LCD_ShowNum(60, 160, phH, 5, 16);
			Show_Str(110, 160, 64, 16, ".", 16, 0); 
			LCD_ShowNum(120, 160, phL, 2, 16);
			
			BUMP_ON;
			delay_ms(1000);
			BUMP_OFF;
			delay_ms(1000);
			
			if(bumpstate) 
			{
				Show_Str(150,180,64,16,"开",16,0);
				sprintf(Bump,"%s","ON");
			}
			else
			{
				Show_Str(150,180,64,16,"关",16,0);
				sprintf(Bump,"%s","OFF");
			}
	
	
			sprintf(fenDegree,"%d",fengspeeddang);
			LCD_ShowNum(110,200,fengspeeddang,2,16); 
			Show_Str(150, 200, 64, 16, "档", 16, 0);
			
	    if(lightState) 
			{
				Show_Str(150,220,64,16,"开",16,0);
				sprintf(lightstr,"%s","ON");
			}
			else
			{
				Show_Str(150,220,64,16,"关",16,0);
				sprintf(lightstr,"%s","OFF");
			}
			
			if(Doorstate) 
			{
				Show_Str(150,240,64,16,"开",16,0);
				sprintf(Doorstr,"%s","ON");
			}
			else
			{
				Show_Str(150,240,64,16,"关",16,0);
				sprintf(Doorstr,"%s","OFF");
			}
			
			
			if(aiwarning) 
			{
				sprintf(AIWarningstr,"%s","slug");
				Show_Str(150,260,64,16,"发现害虫",16,0);
			}
			else
			{
				sprintf(AIWarningstr,"%s","healthy");
				Show_Str(150,260,64,16,"健康",16,0);
			}		
			
			
			
			switch(state){
				case 0:
					sprintf(State,"%s","1");
					Show_Str(150,280,64,16,"1期",16,0);
				break;
				
				case 1:
					sprintf(State,"%s","2");
					Show_Str(150,280,64,16,"2期",16,0);
				break;
				
				case 2:
					sprintf(State,"%s","3");
					Show_Str(150,280,64,16,"3期",16,0);
				break;
				
				case 3:
					sprintf(State,"%s","4");
					Show_Str(150,280,64,16,"4期",16,0);
				break;
			
			}
			
}



void SoilHumidity_Control(void) {
    static uint8_t last_state = 0;
    uint8_t humidity_threshold = 30;  // ????
    
    soilhumi = Soil_Hum_Get_Val();
    
    if(soilhumi < humidity_threshold) {
        if(last_state != 1) {
            BUMP_ON;
            bumpstate = 1;
            last_state = 1;
            printf("Pump ON - Soil humidity: %d%%\n", soilhumi);
        }
    } else {
        if(last_state != 0) {
            BUMP_OFF;
            bumpstate = 0;
            last_state = 0;
            printf("Pump OFF - Soil humidity: %d%%\n", soilhumi);
        }
    }
}




u8 heart=0;

void MQTT_report()
{
		GetSensorData();
		Setup_JSON_Tree_JX();	
		_mqtt.PublishData(MQTT_PUBLISH_TOPIC,A_JSON_Tree,0);
		heart++;
		if(heart>=20) //1分钟时间，发送一个心跳
		{
			heart=0;
				_mqtt.SendHeart();
			}
}
void Do_property_response(char * propertydata)
{
	//如下为网上下发命令后，嵌入式开发部分响应的代码，历史性意义 
	char requestid[37]={0};
	char * start2=NULL;
	char tempres[150]={0};
	u8 i=0;
	start2= strstr((char*)propertydata,"request_id=");
				
	for (i=0;i<36;i++)
			requestid[i]= * (start2+11+i);
					
	strcpy(tempres,MQTT_SETPROPERTY_TOPIC);
	strcat(tempres,requestid);
	_mqtt.PublishData(tempres,RESPONSE_CMD,0);
	printf("request back is %s\r\n",tempres);
}

//来自于华为云或者人工智能赋能侧，对某个变量的属性设置，例如手势的含义0-200等
/*void set_cloud_property(char * propertydata)
{
	if (strstr((char*)propertydata, "AIWarning")) 
					//说明有人工智能属性修改请求
			{
				if(strstr((char*)propertydata, "ON"))
					{
			      printf("set the AIWarning ON\r\n");				//设置调试
				
				    //华为云设置属性成功失败 2026-1-12日 程国辉
						aiwarning =1;//表明AI赋能按键为ON
						

				    Do_property_response(propertydata);
				}
				else if(strstr((char*)propertydata, "OFF"))
				{
						printf("set the AIWarning OFF\r\n");				//设置调试，正式运行后可以删除
						aiwarning =0;//表明AI赋能按键为OFF			
						Do_property_response(propertydata);
			  }
			}
	
}*/

int main(void)
 {	 	  
  
	u8 timeCount=0;
	uint32_t StdId=0;

	delay_init();	    	 //延时函数初始化	  
	my_mem_init(SRAMIN);		//初始化内部内存池
	exfuns_init();				//为fatfs相关变量申请内存  
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);//设置中断优先级分组为组2：2位抢占优先级，2位响应优先级
	usart_Init(115200);	 	//串口初始化为115200
	usart2_Init(115200);	
	usart3_Init(115200);
	printf("Uart 1 ,Uart 2 and Uart 3 init ok\r\n");
 	usmart_dev.init(72);		//初始化USMART		
 	LED_Init();		  			//初始化与LED连接的硬件接口
	KEY_Init();					//初始化按键
	LCD_Init();			   		//初始化LCD   
	LCD_Clear(WHITE);		   	//清屏
 	
	font_init();
	Show_Str(80,50,280,16,"环境监测系统",16,0);	
	Show_Str(30,80,200,16,"温度:",16,0);
	Show_Str(30,100,200,16,"湿度:",16,0);
	Show_Str(30,120,200,16,"亮度:",16,0);
	Show_Str(30,140,200,16,"土壤湿度:",16,0);
	Show_Str(30,160,200,16,"PH值:",16,0);
	Show_Str(30,180,200,16,"水泵状态:",16,0);
	Show_Str(30,200,200,16,"风档状态:",16,0);
	Show_Str(30,220,200,16,"灯的状态:",16,0);
	Show_Str(30,240,200,16,"挡光板状态:",16,0);
	Show_Str(30,260,200,16,"AI病虫害检测:",16,0);
	Show_Str(30,280,200,16,"当前生长周期:",16,0);
	
	Lsens_Init(); 			//初始化光敏传感器
	W25QXX_Init();				//初始化W25Q128
	MQTT_Init();  

	Motor_Init();
	Servo_Init();
	Soil_Hum_Init();
	PH_Init();
	BUMP_Init();
	
	BEEP_Init();
	TIM3_Init(59999,59999);  
	Init_HX711pin();

	
	TIM_Cmd(TIM5, ENABLE);		//TIM5开启定时心跳信号
	 while (1)
	{
		delay_ms(10);
		
		timeCount ++;
		
		if(timeCount >= 100)	//发送间隔3s
		{

			timeCount = 0;
			
			SoilHumidity_Control();
			
			MQTT_report();
			
		}
		
		if((Serial0_RxFlag==1)&&((USART_GetITStatus(USART3, USART_IT_RXNE)==RESET))&&(Uart3_index>=5))//防止心跳信号进入命令行列，导致死机
		{  
			printf("Serial0_RxFlag comein \r\n");				//串口回传一个字符串LED_ON_OK
			printf("USART3_RX_BUF_BK=%s\n",USART3_RX_BUF_BK+5);//因为MQTT协议的头ox00,有个别是零，避免打印不全，经过测试第五个往后打印比较完整

			printf("Uindex=%d\n",Uart3_index);
			//Deal_MQTT_Rxdata();
			
			//如果是下发的信息设置属性数据
			//if(strstr((char*)USART3_RX_BUF_BK+5, "properties/set"))
			//{
			//	set_cloud_property(USART3_RX_BUF_BK+5);
				//continue;
			//}
			
      if (strstr((char*)USART3_RX_BUF_BK+5, "Fengdegree")) //因为MQTT协议的头ox00,有个别是零，避免打印不全，经过测试第四个往后打印比较完整
				//说明有风扇的命令下发
			{
				
				printf("USART3_RX_BUF_BK[132]=%c\r\n",USART3_RX_BUF_BK[132]);
				printf("USART3_RX_BUF_BK[133]=%c\r\n",USART3_RX_BUF_BK[133]);
				printf("USART3_RX_BUF_BK[134]=%c\r\n",USART3_RX_BUF_BK[134]);
				printf("USART3_RX_BUF_BK[135]=%c\r\n",USART3_RX_BUF_BK[135]);
				fengspeeddang =USART3_RX_BUF_BK[134]-'0';
				Motor_FengSpeed(ROUND_TO_UINT16(fengspeeddang*100/7));
				printf("风速发生变化  Fenspeed=%d\r\n",fengspeeddang);				//串口回传一个字符串
				
				 //历史意义的函数，华为云命令反馈函数编写成功
				Do_command_response();
			}
			else if(strstr((char*)USART3_RX_BUF_BK+4, "Light"))
			{
				if(strstr((char*)USART3_RX_BUF_BK+4, "ON"))
					{
						lightState = 1;
						LED0=0;
			      printf("light open\r\n");				//串口回传一个字符串LED_ON_OK	 
				
				    //历史意义的函数，华为云命令反馈函数编写成功
				    Do_command_response();
				}
				else if(strstr((char*)USART3_RX_BUF_BK+4, "OFF"))
				{
					lightState = 0;
					LED0=1;
			    printf("light close\r\n");				//串口回传一个字符串LED_ON_OK	 
				
				  //历史意义的函数，华为云命令反馈函数编写成功
			   	Do_command_response();
			  }
		  }
			
			else if(strstr((char*)USART3_RX_BUF_BK+4, "Board"))
			{
				if(strstr((char*)USART3_RX_BUF_BK+4, "ON"))
					{
						
						Door_ON();
			      printf("Board open\r\n");				
				    Do_command_response();
						
				}
				else if(strstr((char*)USART3_RX_BUF_BK+4, "OFF"))
				{
						
						Door_OFF();
			      printf("Board close\r\n");			
						Do_command_response();
					
			  }
		  }
			
			else if(strstr((char*)USART3_RX_BUF_BK+4, "AIWarning"))
			{
				if(strstr((char*)USART3_RX_BUF_BK+4, "slug"))
					{
						
						Servo_Init();
						Servo_SetAngle(110);
						
						
						aiwarning=1;
						
						
						{
							usart_su_TXpacket[0] = 0XAA;
							usart_su_TXpacket[1] = 0X55;
							usart_su_TXpacket[2] = 0x0A;
							usart_su_TXpacket[3] = aiwarning;
							usart_su_TXpacket[4] = 0x00;
							usart_su_TXpacket[5] = 0x00;
							usart_su_TXpacket[6] = 0x00;
							usart_su_TXpacket[7] = 0X55;
							usart_su_TXpacket[8] = 0XAA;
							usart_send2su_array(usart_su_TXpacket, 9);
						}
						
			      printf("AIWarning: slug \r\n");				
				    Do_command_response();
						
				}
				else if(strstr((char*)USART3_RX_BUF_BK+4, "healthy"))
				{
						
						Servo_Init();
		   	
						Servo_SetAngle(0);
					
					
						//找到具体语音索引，播放对应语音
						
						aiwarning =0;
						{
							usart_su_TXpacket[0] = 0XAA;
							usart_su_TXpacket[1] = 0X55;
							usart_su_TXpacket[2] = 0x0B;
							usart_su_TXpacket[3] = aiwarning;
							usart_su_TXpacket[4] = 0x00;
							usart_su_TXpacket[5] = 0x00;
							usart_su_TXpacket[6] = 0x00;
							usart_su_TXpacket[7] = 0X55;
							usart_su_TXpacket[8] = 0XAA;
							usart_send2su_array(usart_su_TXpacket, 9);
						}
						
			      printf("AIWarning: healthy \r\n");				
				    Do_command_response();
					
			  }
		  }
			
			else if(strstr((char*)USART3_RX_BUF_BK+4, "State"))
			{
				//测试命令下发的参数位置：在USART3_RX_BUF_BK基础上加上4
				printf("USART3_RX_BUF_BK[132]=%c\r\n",USART3_RX_BUF_BK[132]);
				printf("USART3_RX_BUF_BK[133]=%c\r\n",USART3_RX_BUF_BK[133]);
				printf("USART3_RX_BUF_BK[134]=%c\r\n",USART3_RX_BUF_BK[134]);
				printf("USART3_RX_BUF_BK[135]=%c\r\n",USART3_RX_BUF_BK[135]);
				aiwarning =USART3_RX_BUF_BK[134]-'0';
				
				//找到具体语音索引，播放对应语音
				
				//待写
				
				{
					usart_su_TXpacket[0] = 0XAA;
					usart_su_TXpacket[1] = 0X55;
					usart_su_TXpacket[2] = 0x0B;
					usart_su_TXpacket[3] = state;
					usart_su_TXpacket[4] = 0x00;
					usart_su_TXpacket[5] = 0x00;
					usart_su_TXpacket[6] = 0x00;
					usart_su_TXpacket[7] = 0X55;
					usart_su_TXpacket[8] = 0XAA;
					usart_send2su_array(usart_su_TXpacket, 9);
				}
				printf("播放语音为  state=%d\r\n",state);				//串口回传一个字符串
				
				 //历史意义的函数，华为云命令反馈函数编写成功
				Do_command_response();
				
		  }
		
			Serial0_RxFlag = 0;			//处理完成后，需要将接收数据包标志位清零，否则将无法接收后续数据包
			Uart3_index=0;
			delay_ms(200);
			memset(USART3_RX_BUF_BK,0,USART3_RXBUFF_SIZE);
			memset(USART3_RX_BUF,0,USART3_RXBUFF_SIZE);
		}
//		delay_ms(10);
	}
}

void Do_command_response()
{
	//如下为网上下发命令后，嵌入式开发部分响应的代码，历史性意义 
	char requestid[37]={0};
	char * start2=NULL;
	char tempres[150]={0};
	u8 i=0;
	start2= strstr((char*)USART3_RX_BUF_BK+4,"request_id=");
				
	for (i=0;i<36;i++)
			requestid[i]= * (start2+11+i);
					
	strcpy(tempres,MQTT_COMMANDRESP_TOPIC);
	strcat(tempres,requestid);
	_mqtt.PublishData(tempres,RESPONSE_CMD,0);
	printf("request back is %s\r\n",tempres);
				
				//以上为网上下发命令后，嵌入式开发部分响应的代码，历史性意义 
}



//连接热点、接入华为云
void MQTT_Init(void)
{
	status=0;
	if(_net.Init(USART_RX_BUF,sizeof(USART_RX_BUF),txbuf,sizeof(txbuf))!=0) 	//检查ESP8266
	{
		printf("Net Init OK!\r\n");
		status++;
	}
	else
	{
		printf("Net Init Error!\r\n");
		status=0;
	}
	if(status==1)  		//连接热点
	{
		if(_net.ConnectAP(WIFI_NAME,WIFI_PASSWD)!=0)
		{
			printf("Conncet AP OK!\r\n");
			status++;
		}
		else 
		{
			printf("Conncet AP Error!\r\n");
			status=0;
		}
	}
	if(status==2)      //连接TCP
	{	
		if(_net.ConnectServer("TCP",MQTT_BROKERADDRESS,1883)!=0) //39.107.71.77   //47.105.141.194 //1883
		{
			printf("Conncet Server OK!\r\n");
			status++;
		}
		else
		{
			printf("Conncet Server Error!\r\n");	
		}
	}
	if(status==3)  	//登录MQTT	
	{
		_mqtt.Init(USART3_RX_BUF,sizeof(USART3_RX_BUF),txbuf,sizeof(txbuf));
		if(_mqtt.Connect(
		MQTT_CLIENTID,  //ClientID
		MQTT_USARNAME,  //Username
		MQTT_PASSWD       //Password
		) != 0)
		{
			printf("Enter MQTT OK!\r\n");

			status++;
		}
		else
		{
			printf("Enter MQTT Error!\r\n");
			status++;
		}	
	}	
	if(status==4)  		//订阅主题
	{
		if(_mqtt.SubscribeTopic(MQTT_COMMANDRESP_TOPIC,0,1) != 0)
		{
			printf("SubscribeTopic OK!\r\n");
		}
		else
		{			
			printf("SubscribeTopic Error!\r\n");
		}
		
//		if(_mqtt.SubscribeTopic(MQTT_SETPROPERTY_TOPIC,0,1) != 0)
//		{
//			printf("SubscribeTopic OK!\r\n");
//		}
//		else
//		{			
//			printf("SubscribeTopic Error!\r\n");
//		}
		
		if(_mqtt.SubscribeTopic(MQTT_PUBLISH_TOPIC,0,1) != 0)
		{
			printf("MQTT_PUBLISH_TOPIC OK!\r\n");
		}
		else
		{			
			printf("MQTT_PUBLISH_TOPIC Error!\r\n");
		}	
	}
	
	status = 0;
}














