#include "beep.h"



void BEEP_Init(void)
{

	 
	 GPIO_InitTypeDef  GPIO_InitStructure;
		
	 RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB, ENABLE);	 //使能PA|PB端口时钟
		
	 GPIO_InitStructure.GPIO_Pin = GPIO_Pin_8;				 //PA4|5 端口配置
	 GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP; 		 //推挽输出
	 GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;		 //IO口速度为50MHz
	 GPIO_Init(GPIOB, &GPIO_InitStructure);					 //根据设定参数初始化GPIOA4|5
	
	
	
}


void BEEP_ON(void)
{
	GPIO_SetBits(GPIOB,GPIO_Pin_8);	
}

void BEEP_OFF(void)
{
	GPIO_ResetBits(GPIOB,GPIO_Pin_8);
}
