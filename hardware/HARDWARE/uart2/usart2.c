#include "usart2.h"
#include "usart.h"
#include "stm32f10x.h"

#include "stdarg.h"
#include "string.h"
#include "stdio.h"
#include "delay.h"
#include "LED.h"
#include "dht11.h"
#include "adc.h"
#include "lsens.h"

#include "math.h"
#include "motor.h"
#include "led.h"
#include "Servo.h"
#include "bump.h"

#define ROUND_TO_UINT16(x)   ((uint16_t)(x)+0.5)>(x)? ((uint16_t)(x)):((uint16_t)(x)+1) //将浮点数四舍五入，转换为uint16_t


uint8_t usart_su_RXdata;		//存放接收数据寄存器的值
uint8_t usart_su_RXflag;		//接收数据标志位
uint8_t usart_su_RXpacket[6] = {0};	//hex数据包接收数组
uint8_t usart_su_TXpacket[14] = {0};	//hex数据包接收数组
extern  uint8_t adcx;
extern  uint8_t angle;
extern  uint8_t fengspeeddang;
extern  uint8_t Doorstate;
extern  uint8_t lightState;
extern  uint8_t bumpstate;
extern u8 sendbuf[8]; //can发送缓冲数据 
uint8_t send_state_buf[6]={0};
uint8_t voicest=0;

uint8_t temph,templ,humih,humil;   //温湿度
uint8_t lumi;
uint32_t weight;

/************************************
									*
	功能：串口2初始化  发送 接收	*
	参数：bound 波特率				*
									*									
*************************************/
void usart2_Init(unsigned int bound)
{  	 	
    GPIO_InitTypeDef GPIO_InitStructure;     //定义一个设置GPIO功能的变量
	USART_InitTypeDef USART_InitStructure;   //定义一个设置串口功能的变量
	NVIC_InitTypeDef NVIC_InitStructure;     //如果使能接收功能，定义一个设置中断的变量

	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);    //设置中断向量分组：第2组 抢先优先级：0 1 2 3 子优先级：0 1 2 3	
      
	
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE); //使能串口2时钟
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);  //使能GPIOA时钟
	USART_DeInit(USART2);                                  //串口2寄存器重新设置为默认值
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_2;              //准备设置PA2
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;      //IO速率50M
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	       //复用推挽输出，用于串口2的发送
    GPIO_Init(GPIOA, &GPIO_InitStructure);                 //设置PA2
   
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_3;              //准备设置PA3/改PA7试试
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;  //浮空输入，用于串口2的接收
	//GPIO_InitStructure.GPIO_Mode =GPIO_Mode_IPU;
    GPIO_Init(GPIOA, &GPIO_InitStructure);                 //设置PA3
	
	USART_InitStructure.USART_BaudRate = bound;  /*bound*/                                  //波特率设置
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;                    //8个数据位
	USART_InitStructure.USART_StopBits = USART_StopBits_1;                         //1个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;                            //无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Tx | USART_Mode_Rx;	               //收发模式
      
    USART_Init(USART2, &USART_InitStructure);                                      //设置串口2	

	USART_ClearFlag(USART2, USART_FLAG_RXNE);	              //清除接收标志位
	USART_ITConfig(USART2, USART_IT_RXNE, ENABLE);            //开启接收中断
    NVIC_InitStructure.NVIC_IRQChannel = USART2_IRQn;         //设置串口2中断
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 3; //抢占优先级0
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 2;		  //子优先级0
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			  //中断通道使能
	NVIC_Init(&NVIC_InitStructure);	                          //设置串口2中断 

	USART_Cmd(USART2, ENABLE);   
}


/************************************
									*
	功能：串口2接收中断函数			*
									*									
*************************************/
//USART_IT_PE 奇偶错误中断

//USART_IT_TXE 发送中断

//USART_IT_TC 发送完成中断

//USART_IT_RXNE 接收中断

//USART_IT_IDLE 空闲总线中断

//USART_IT_LBD LIN中断检测中断

//USART_IT_CTS CTS中断

//USART_IT_ERR 错误中断

//串口2中断，接收输出调试信息
void USART2_IRQHandler(void)
{
	uint8_t RX_su_State,su_pRXpacket;
	//usart_init();
	if(USART_GetITStatus(USART2, USART_IT_RXNE) == SET){
		
 
		usart_su_RXdata = USART_ReceiveData(USART2);
		printf("info=%x \n",usart_su_RXdata);
		//判断接收的数据包头
		if(RX_su_State == 0){	
			if(usart_su_RXdata == 0XAA){	//接收到的使hex文件
				usart_su_RXpacket[0] = usart_su_RXdata;
				RX_su_State = 1;	
				su_pRXpacket = 1;
			}
		}
			else if(RX_su_State == 1){	//接收hex文件的内容
				usart_su_RXpacket[su_pRXpacket] = usart_su_RXdata;
				su_pRXpacket++;
				if(su_pRXpacket >= 5){		//接收数据包长度位4的数据
					RX_su_State = 2;
				}
			}
			else if(RX_su_State == 2){	//判断hex数据包结束标志位
				if(usart_su_RXdata == 0XAA){
					//Usart1_SendBuf(usart_su_RXpacket,6);
					usart_su_RXpacket[su_pRXpacket] = usart_su_RXdata;
					RX_su_State = 0;
					usart_su_RXflag = 1;
					USART_ClearITPendingBit(USART2, USART_IT_RXNE);
				}
				else{
					RX_su_State = 0;
					usart_su_RXflag = 0;
					//Usart1_SendBuf(usart_su_RXpacket,6);
					usart_su_RXpacket[su_pRXpacket] = usart_su_RXdata;
					//usart_send_string("$");
				}
			}
	}
	
	vioce_analysis(usart_su_RXdata);
	usart_su_RXflag =1;
}

/**
  * @brief  获取usart_su_flag的值
  * @param  None
  * @retval usart_su_RXflag 串口2接收标志位 
  */
uint8_t usart_get_su_RXflag(void)
	{
	if(usart_su_RXflag == 1){
		usart_su_RXflag = 0;
		return 1;
	}
	return 0;
}
	
/**
  * @brief  发送单个字节
* @param  uint8_t data	需要发送的字节
  * @retval None
  */
void usart_send2su_bit(uint8_t data){
	while(USART_GetFlagStatus(USART2, USART_FLAG_TC) != 1); //等待上一次数据发送完成
	USART_SendData(USART2, data);	//发送数据
}


/**
	* @brief  发送一个数组
	* @param  uint8_t *array 需要发送的数组
	* @param	uint16_t len	发送的数组长度
  * @retval None
  */
void usart_send2su_array(uint8_t *array, uint16_t len){
	uint16_t i = 0;
	for(i = 0; i < len; i++){
		usart_send2su_bit(array[i]);
	}
}
 


/**
  * @brief  处理su_03t的数据
  * @param  None
  * @retval None
  */
void vioce_analysis(uint8_t info){
		printf("usart_su_RXpacket=%x \n",usart_su_RXpacket[3]);
		switch(info){
			case 0x01:
				LED0=0;
				lightState =1;
			break;
			case 0x02:
				LED0=1;
				lightState =0;
			break;
			case 0x03:
				Motor_Init();
				if(fengspeeddang<=8) fengspeeddang++;
				Motor_FengSpeed(ROUND_TO_UINT16(fengspeeddang*100/7));
				usart_su_TXpacket[0] = 0XAA;
				usart_su_TXpacket[1] = 0X55;
				usart_su_TXpacket[2] = 0x01;
				usart_su_TXpacket[3] = fengspeeddang;
				usart_su_TXpacket[4] = 0x00;
				usart_su_TXpacket[5] = 0x00;
				usart_su_TXpacket[6] = 0x00;
				usart_su_TXpacket[7] = 0X55;
				usart_su_TXpacket[8] = 0XAA;;
				usart_send2su_array(usart_su_TXpacket, 9);
				printf("FENGDANG:%d",fengspeeddang);
				break;
			case 0x04:
				Motor_Init();
				if(fengspeeddang>0)fengspeeddang--;
				Motor_FengSpeed(ROUND_TO_UINT16(fengspeeddang*100/7));
				usart_su_TXpacket[0] = 0XAA;
				usart_su_TXpacket[1] = 0X55;
				usart_su_TXpacket[2] = 0x02;
				usart_su_TXpacket[3] = fengspeeddang;
				usart_su_TXpacket[4] = 0x00;
				usart_su_TXpacket[5] = 0x00;
				usart_su_TXpacket[6] = 0x00;
				usart_su_TXpacket[7] = 0X55;
				usart_su_TXpacket[8] = 0XAA;
				usart_send2su_array(usart_su_TXpacket, 9);
				printf("FENGDANG:%d",fengspeeddang);
				break;
			case 0x05:
				Servo_Init();
				Servo_SetAngle(110);
				Doorstate=1;
				printf("angle:%d",angle);
				printf("DoorState:%d",Doorstate);
				break;
			case 0x06:
				Servo_Init();
				Servo_SetAngle(0);
				Doorstate=0;
				printf("angle:%d",angle);
				printf("DoorState:%d",Doorstate);
				break;
				
			case 0x07:
				BUMP_ON;
				bumpstate =1;
			break;
			case 0x08:
				BUMP_OFF;
				bumpstate =0;
			break;
			
			case 0x0A://询问当前温度
				DHT11_Read_Data(&humih,&humil,&temph,&templ);
				usart_su_TXpacket[0] = 0XAA;
				usart_su_TXpacket[1] = 0X55;
				usart_su_TXpacket[2] = 0x03;
				usart_su_TXpacket[3] = temph;
				usart_su_TXpacket[4] = 0x00;
				usart_su_TXpacket[5] = 0x00;
				usart_su_TXpacket[6] = 0x00;
				usart_su_TXpacket[7] = templ;
				usart_su_TXpacket[8] = 0x00;
				usart_su_TXpacket[9] = 0x00;
				usart_su_TXpacket[10] = 0x00;
				usart_su_TXpacket[11] = 0X55;
				usart_su_TXpacket[12] = 0XAA;
				usart_send2su_array(usart_su_TXpacket, 13);
				break;
			case 0x0B://询问当前湿度
				DHT11_Read_Data(&humih,&humil,&temph,&templ);
				usart_su_TXpacket[0] = 0XAA;
				usart_su_TXpacket[1] = 0X55;
				usart_su_TXpacket[2] = 0x04;
				usart_su_TXpacket[3] = humih;
				usart_su_TXpacket[4] = 0x00;
				usart_su_TXpacket[5] = 0x00;
				usart_su_TXpacket[6] = 0x00;
				usart_su_TXpacket[7] = humil;
				usart_su_TXpacket[8] = 0x00;
				usart_su_TXpacket[9] = 0x00;
				usart_su_TXpacket[10] = 0x00;
				usart_su_TXpacket[11] = 0X55;
				usart_su_TXpacket[12] = 0XAA;
				usart_send2su_array(usart_su_TXpacket, 13);
				break;
			case 0x0C:
				Lsens_Get_Val();
				usart_su_TXpacket[0] = 0XAA;
				usart_su_TXpacket[1] = 0X55;
				usart_su_TXpacket[2] = 0x05;
				usart_su_TXpacket[3] = adcx;
				usart_su_TXpacket[4] = 0x00;
				usart_su_TXpacket[5] = 0x00;
				usart_su_TXpacket[6] = 0x00;
				usart_su_TXpacket[7] = 0X55;
				usart_su_TXpacket[8] = 0XAA;;
				usart_send2su_array(usart_su_TXpacket, 9);
				break;
			

	}
}

 




