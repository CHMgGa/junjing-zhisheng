#ifndef __PH_H
#define	__PH_H
#include "stm32f10x.h"
#include "adc.h"
#include "delay.h"
#include "math.h"

/*****************辰哥单片机设计******************
											STM32
 * 文件			:	PH传感器h文件                   
 * 版本			: V1.0
 * 日期			: 2025.2.4
 * MCU			:	STM32F103C8T6
 * 接口			:	见代码				
 * IP账号		:	辰哥单片机设计（同BILIBILI|抖音|快手|小红书|CSDN|公众号|视频号等）
 * 作者			:	辰哥 
 * 工作室		: 异方辰电子工作室
 * 讲解视频	:	https://www.bilibili.com/video/BV1NrNweHEQx/?share_source=copy_web
 * 官方网站	:	www.yfcdz.cn

**********************BEGIN***********************/

#define PH_READ_TIMES	10  //PH传感器ADC循环读取次数

/***************根据自己需求更改****************/
// PH GPIO宏定义

#define		PH_GPIO_CLK								RCC_APB2Periph_GPIOA
#define 	PH_GPIO_PORT							GPIOA
#define 	PH_GPIO_PIN								GPIO_Pin_4
#define   ADC_CHANNEL								ADC_Channel_4	// ADC 通道宏定义


/*********************END**********************/


void PH_Init(void);
float PH_GetData(void);
void PH_Read_Data(uint8_t *phH, uint8_t *phL);
#endif /* __PH_H */

