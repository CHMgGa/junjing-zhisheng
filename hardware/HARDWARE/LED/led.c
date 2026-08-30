#include "led.h"
#include "delay.h"

//////////////////////////////////////////////////////////////////////////////////	 
//本程序只供学习使用，未经作者许可，不得用于其它任何用途
//ALIENTEK精英STM32开发板
//LED驱动代码	   
//正点原子@ALIENTEK
//技术论坛:www.openedv.com
//修改日期:2012/9/2
//版本：V1.0
//版权所有，盗版必究。
//Copyright(C) 广州市星翼电子科技有限公司 2009-2019
//All rights reserved									  
////////////////////////////////////////////////////////////////////////////////// 	   

//初始化PB5和PE5为输出口.并使能这两个口的时钟		    
//LED IO初始化
void LED_Init(void)
{
 
	 GPIO_InitTypeDef  GPIO_InitStructure;
		
	 RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA|RCC_APB2Periph_GPIOB|RCC_APB2Periph_GPIOF|RCC_APB2Periph_GPIOG|RCC_APB2Periph_GPIOC, ENABLE);	 //使能PA|PB端口时钟
		
	 GPIO_InitStructure.GPIO_Pin = GPIO_Pin_5|GPIO_Pin_4;				 //PA45 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOA, &GPIO_InitStructure);					 //根据设定参数初始化GPIOA4|5
	 GPIO_SetBits(GPIOA,GPIO_Pin_5|GPIO_Pin_4);						 //PB.5 输出高
	
	 GPIO_InitStructure.GPIO_Pin =GPIO_Pin_1|GPIO_Pin_2;				 //PB1 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOB, &GPIO_InitStructure);					 //根据设定参数初始化GPIOB1
	 GPIO_SetBits(GPIOB,GPIO_Pin_1|GPIO_Pin_2);
	
	
	 GPIO_InitStructure.GPIO_Pin =GPIO_Pin_0|GPIO_Pin_1|GPIO_Pin_2|GPIO_Pin_3|GPIO_Pin_4|GPIO_Pin_5|GPIO_Pin_10|GPIO_Pin_11|GPIO_Pin_12|GPIO_Pin_13|GPIO_Pin_14|GPIO_Pin_15;				 //PE11 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOF, &GPIO_InitStructure);					 //根据设定参数初始化GPIOF11
	 GPIO_SetBits(GPIOF,GPIO_Pin_0|GPIO_Pin_1|GPIO_Pin_2|GPIO_Pin_3|GPIO_Pin_4|GPIO_Pin_5|GPIO_Pin_11|GPIO_Pin_12|GPIO_Pin_13|GPIO_Pin_14|GPIO_Pin_15);
 
 
	 GPIO_InitStructure.GPIO_Pin =GPIO_Pin_2|GPIO_Pin_5;				 //PE11 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOC, &GPIO_InitStructure);					 //根据设定参数初始化GPIOF11
	 GPIO_SetBits(GPIOC,GPIO_Pin_2|GPIO_Pin_5);
	 
	 GPIO_InitStructure.GPIO_Pin =GPIO_Pin_5;				 //PE11 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOB, &GPIO_InitStructure);					 //根据设定参数初始化GPIOF11
	 GPIO_SetBits(GPIOB,GPIO_Pin_5);



}
 

void fun1(u8 carOn)
{
  
	if (carOn==1)
	{
		GPIO_SetBits(GPIOA,GPIO_Pin_5); 
	  GPIO_SetBits(GPIOC,GPIO_Pin_5); 
	}
	else
	{
		GPIO_ResetBits(GPIOA,GPIO_Pin_5); 
	  GPIO_ResetBits(GPIOC,GPIO_Pin_5); 
	}
}

void Front_Lamp(u8 frog)
{
	if (frog==1)
		GPIO_SetBits(GPIOB,GPIO_Pin_1); 
	else
		GPIO_ResetBits(GPIOB,GPIO_Pin_1); 
	 
}
void AC_Lamp(u8 AC)
{
	if (AC==1)
		GPIO_SetBits(GPIOB,GPIO_Pin_2); 
	else
		GPIO_ResetBits(GPIOB,GPIO_Pin_2); 
	 
}
void foot_Lamp(u8 foot)
{
	if (foot==1)
		GPIO_SetBits(GPIOF,GPIO_Pin_11); 
	else
		GPIO_ResetBits(GPIOF,GPIO_Pin_11); 
	 
}



void LED1_ON(void)
{
	
	GPIO_ResetBits(GPIOC,GPIO_Pin_2); 
}
void LED2_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_12); 
}

void LED3_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_15); 
}
void LED4_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_14); 
}
void LED5_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_13); 
	
}


void LED6_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_0); 
}
void LED7_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_1); 
}

void LED8_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_2);  
}
void LED9_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_3); 
}
void LED10_ON(void)
{
	
	GPIO_ResetBits(GPIOF,GPIO_Pin_4); 
	
}


void LED1_OFF(void)
{
	
	GPIO_SetBits(GPIOC,GPIO_Pin_2); 
}

void LED2_OFF(void)
{
	GPIO_SetBits(GPIOF,GPIO_Pin_12); 
}
void LED3_OFF(void)
{
	
	GPIO_SetBits(GPIOF,GPIO_Pin_15); 
}
void LED4_OFF(void)
{
	
	GPIO_SetBits(GPIOF,GPIO_Pin_14); 
}
void LED5_OFF(void)
{
	GPIO_SetBits(GPIOF,GPIO_Pin_13); 
	
}

void LED6_OFF(void)
{
	
	GPIO_SetBits(GPIOF,GPIO_Pin_0); 
}

void LED7_OFF(void)
{
	
	GPIO_SetBits(GPIOF,GPIO_Pin_1); 
}
void LED8_OFF(void)
{
	
	
	GPIO_SetBits(GPIOF,GPIO_Pin_2); 
}
void LED9_OFF(void)
{
	
	GPIO_SetBits(GPIOF,GPIO_Pin_3); 
}
void LED10_OFF(void)
{
	GPIO_SetBits(GPIOF,GPIO_Pin_4); 
	
}


void fenwei_Lamp(void)
{
	
	LED1_ON();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	LED6_ON();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_ON();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_ON();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_ON();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_ON();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_ON();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_ON();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_ON();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_ON();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_ON();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_ON();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_ON();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_ON();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_ON();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_ON();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_ON();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_ON();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_ON();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	 LED6_ON();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	LED1_OFF();
	LED2_OFF();
	LED3_OFF();
	LED4_OFF();
	LED5_OFF();
	LED6_OFF();
	LED7_OFF();
	LED8_OFF();
	LED9_OFF();
	LED10_OFF();
	delay_ms(100);
	
	
}
