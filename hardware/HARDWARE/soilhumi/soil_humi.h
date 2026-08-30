#ifndef __SOIL_HUM_H
#define __SOIL_HUM_H	
#include "sys.h" 
#include "adc.h" 

/********************* 可配置参数（根据硬件修改） *********************/
#define SOIL_HUM_READ_TIMES	10		// 采样次数（多次平均降噪声）
#define SOIL_HUM_ADC_CHX	ADC_Channel_7	// 土壤湿度ADC通道（对应PF9）
#define SOIL_HUM_GPIO_PIN	GPIO_Pin_9		// 传感器AO引脚（PF9）
#define SOIL_HUM_GPIO_PORT	GPIOF			// 传感器AO引脚端口
#define SOIL_HUM_GPIO_CLK	RCC_APB2Periph_GPIOF	// 引脚时钟

/********************* 函数声明（与lsens接口一致） *********************/
void Soil_Hum_Init(void); 				// 初始化土壤湿度传感器
u8 Soil_Hum_Get_Val(void);				// 读取湿度值（0=完全干，100=完全湿）
#endif 