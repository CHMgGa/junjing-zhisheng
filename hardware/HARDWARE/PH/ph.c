#include "ph.h"
#include "adc.h"

/*****************辰哥单片机设计******************
											STM32
 * 文件			:	PH传感器c文件                   
 * 版本			: V1.0
 * 日期			: 2024.2.4
 * MCU			:	STM32F103C8T6
 * 接口			:	见代码				
 * IP账号		:	辰哥单片机设计（同BILIBILI|抖音|快手|小红书|CSDN|公众号|视频号等）
 * 作者			:	辰哥 
 * 工作室		: 异方辰电子工作室
 * 讲解视频	:	https://www.bilibili.com/video/BV1NrNweHEQx/?share_source=copy_web
 * 官方网站	:	www.yfcdz.cn

**********************BEGIN***********************/

void PH_Init(void)
{
	{
		GPIO_InitTypeDef GPIO_InitStructure;
		
		RCC_APB2PeriphClockCmd (PH_GPIO_CLK, ENABLE );	// 打开 ADC IO端口时钟
		GPIO_InitStructure.GPIO_Pin = PH_GPIO_PIN;					// 配置 ADC IO 引脚模式
		GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AIN;		// 设置为模拟输入
		
		GPIO_Init(PH_GPIO_PORT, &GPIO_InitStructure);				// 初始化 ADC IO

		T_Adc_Init();
	}

}


uint16_t PH_ADC_Read(void)
{
	//设置指定ADC的规则组通道，采样时间
	return T_Get_Adc(ADC_CHANNEL);
}

float PH_GetData(void)
{
	float  tempData = 0;
	float PH_DAT;
	uint8_t i;
	tempData = T_Get_Adc_Average(ADC_CHANNEL, PH_READ_TIMES);
	
	PH_DAT = (tempData/4096.0)*3.3;
	PH_DAT = -5.7541*PH_DAT+16.654;
	
	if(PH_DAT>14.0)PH_DAT=14.0;
	if(PH_DAT<0)PH_DAT=0.0;
	
	return PH_DAT;

}
void PH_Read_Data(uint8_t *phH, uint8_t *phL)
{
    float ph_value = PH_GetData();
    uint16_t ph_int = (uint16_t)ph_value;  // ????
    uint16_t ph_dec = (uint16_t)((ph_value - ph_int) * 100);  // ????(??2?)
    
    *phH = (uint8_t)ph_int;
    *phL = (uint8_t)ph_dec;
}


