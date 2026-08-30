
#include "usart3.h"
#include "stm32f10x.h"
#include"stdarg.h"
#include "string.h"
#include "stdio.h"
#include "delay.h"

char USART3_RX_STA = 0;            //定义一个变量 0：表示接收未完成 1：表示接收完成 
char USART3_RX_Flag =0;
unsigned int Usart3_RxCounter = 0;      //定义一个变量，记录串口2总共接收了多少字节的数据
extern char USART3_RX_BUF[USART3_RXBUFF_SIZE]; //定义一个数组，用于保存串口2接收到的数据 

extern char USART3_RX_BUF_BK[USART3_RXBUFF_SIZE]; //定义一个数组，用于保存串口2接收到的数据备份，单独处理
extern u16 Uart3_index;
extern u8 Serial0_RxFlag;

/************************************
									*
	功能：串口2初始化  发送 接收	*
	参数：bound 波特率				*
									*									
*************************************/
void usart3_Init(unsigned int bound)
{  	 	
    GPIO_InitTypeDef GPIO_InitStructure;     //定义一个设置GPIO功能的变量
	USART_InitTypeDef USART_InitStructure;   //定义一个设置串口功能的变量
	NVIC_InitTypeDef NVIC_InitStructure;     //如果使能接收功能，定义一个设置中断的变量

	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);    //设置中断向量分组：第2组 抢先优先级：0 1 2 3 子优先级：0 1 2 3	
      
	
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE); //使能串口2时钟
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB, ENABLE);  //使能GPIOA时钟
	USART_DeInit(USART3);                                  //串口2寄存器重新设置为默认值
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;              //准备设置PA2
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;      //IO速率50M
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	       //复用推挽输出，用于串口2的发送
    GPIO_Init(GPIOB, &GPIO_InitStructure);                 //设置PA2
   
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_11;              //准备设置PA3/改PA7试试
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;  //浮空输入，用于串口2的接收
	//GPIO_InitStructure.GPIO_Mode =GPIO_Mode_IPU;
    GPIO_Init(GPIOB, &GPIO_InitStructure);                 //设置PA3
	
	USART_InitStructure.USART_BaudRate = bound;  /*bound*/                                  //波特率设置
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;                    //8个数据位
	USART_InitStructure.USART_StopBits = USART_StopBits_1;                         //1个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;                            //无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Tx | USART_Mode_Rx;	               //收发模式
      
    USART_Init(USART3, &USART_InitStructure);                                      //设置串口2	

	USART_ClearFlag(USART3, USART_FLAG_RXNE);	              //清除接收标志位
	USART_ITConfig(USART3, USART_IT_RXNE, ENABLE);            //开启接收中断
    NVIC_InitStructure.NVIC_IRQChannel = USART3_IRQn;         //设置串口2中断
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 3; //抢占优先级0
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 2;		  //子优先级0
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			  //中断通道使能
	NVIC_Init(&NVIC_InitStructure);	                          //设置串口2中断 

	USART_Cmd(USART3, ENABLE);   
}




/****************************************************
													*
	功能：串口3打印函数								*
	参数：char* fmt,...  格式化输出字符串和参数		*
													*									
*****************************************************/
__align(8) char USART3_TxBuff[USART3_TXBUFF_SIZE];  

void usart3_printf(char* fmt, ...) 
{  
	unsigned int i, length;
	
	va_list ap;
	va_start(ap, fmt);
	vsprintf(USART3_TxBuff, fmt, ap);
	va_end(ap);	
	
	length=strlen((const char*)USART3_TxBuff);		
	while((USART3->SR&0X40) == 0);
	for(i = 0; i < length; i++)
	{			
		USART3->DR = USART3_TxBuff[i];
		while((USART3->SR&0X40) == 0);	
	}	
}



/************************************
									*
	功能：串口2发送函数				*
	参数：data 要发送的数据			*
									*									
*************************************/
void usart3_TxData(unsigned char *data)
{
	int	i;	
	while((USART3->SR&0X40) == 0);
	for(i = 1; i <= (data[0] * 256 + data[1]); i++)
	{			
		USART3->DR = data[i+1];
		delay_ms(10);
		while((USART3->SR&0X40) == 0);
	}
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

    
/************************************
									*
	功能：串口3接收中断深入			*
									*									
*************************************/
//读取一串数据的函数，经过判断后为MQTT数据，则加以主动触发发送给
//MQTT 命令等反馈程序 
//int USART3_RecieveData(unsigned char val)
//{
//	MQTT_RX_BUF
//}


void USART3_IRQHandler(void)                	//串口2中断服务程序
{
	u8 Res;
   

	
	if(USART_GetITStatus(USART3, USART_IT_RXNE) != RESET)  //接收中断(接收到的数据必须是0x0d 0x0a结尾)
		{
		Res =USART_ReceiveData(USART3);	//读取接收到的数据
		
		USART_SendData(USART1,Res); //串口提示数据
		USART3_RX_BUF_BK[Uart3_index++]=Res ;
		while(USART_GetFlagStatus(USART3, USART_FLAG_TXE) == RESET){} ;			
			
		if((USART3_RX_STA&0x8000)==0)//接收未完成
			{
			if(USART3_RX_STA&0x4000)//接收到了0x0d
				{
				if(Res!=0x0a){
					USART3_RX_STA=0;//接收错误,重新开始
					USART_ClearFlag(USART3,USART_IT_RXNE);
					Serial0_RxFlag=1;	
				}
				else USART3_RX_STA|=0x8000;	//接收完成了 
				}
			else //还没收到0X0D
				{	
				if(Res==0x0d)USART3_RX_STA|=0x4000;
				else
					{
					USART3_RX_BUF[USART3_RX_STA&0X3FFF]=Res ;
					
						
					USART3_RX_STA++;
					if(USART3_RX_STA>(USART3_RXBUFF_SIZE-1))
						{
							USART3_RX_STA=0;//接收数据错误,重新开始接收
							USART_ClearFlag(USART3,USART_IT_RXNE);			
						Serial0_RxFlag=1;								
						}
					}	
									
				}
			}   	
		Serial0_RxFlag=1;				
     } 
 }

//串口2发送一个字节
void Usart3_SendByte(u8 val)
{
    USART_SendData(USART3, val);
	//usart2_TxData((char*)val);
	delay_ms(5);
    while (USART_GetFlagStatus(USART3, USART_FLAG_TC) == RESET);	//等待发送完成
}
//串口3发送一个数据包
void Usart3_SendBuf(u8 *buf,u16 len)
{
    while(len--)	Usart3_SendByte(*buf++);
	//usart2_TxData(buf);
}

//串口3发送一个字符串
void Usart3_SendString(u8 *str)
{
    while(*str)	Usart3_SendByte(*str++);
}




