#ifndef __BUMP_H
#define	__BUMP_H
#include "stm32f10x.h"
#include "delay.h"
#include "sys.h"

/*****************辰哥单片机设计******************
											STM32
 * 文件			:	5V水泵模块h文件                   
 * 版本			: V1.0
 * 日期			: 2024.9.22
 * MCU			:	STM32F103C8T6
 * 接口			:	见代码											
 * IP账号		:	辰哥单片机设计（同BILIBILI|抖音|快手|小红书|CSDN|公众号|视频号等）
 * 作者			:	辰哥 
 * 工作室		: 异方辰电子工作室
 * 讲解视频	:	https://www.bilibili.com/video/BV1g6tDedEn6/?share_source=copy_web
 * 官方网站	:	www.yfcdz.cn

**********************BEGIN***********************/

/***************根据自己需求更改****************/
// 水泵模块 GPIO宏定义

#define	BUMP_CLK							RCC_APB2Periph_GPIOA

#define BUMP_GPIO_PIN 				GPIO_Pin_5

#define BUMP_GPIO_PROT 				GPIOA

#define BUMP_ON 		GPIO_SetBits(BUMP_GPIO_PROT,BUMP_GPIO_PIN)
#define BUMP_OFF 	GPIO_ResetBits(BUMP_GPIO_PROT,BUMP_GPIO_PIN)

/*********************END**********************/

void BUMP_Init(void);

#endif



