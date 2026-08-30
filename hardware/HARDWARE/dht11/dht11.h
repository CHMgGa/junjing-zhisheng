#ifndef __DHT11_H
#define __DHT11_H	
//#include "sys.h"

#define DHT        GPIO_Pin_11
#define DHT_PROT   GPIOG

#define OUT 1
#define INT 0

#define DHT_High   GPIO_SetBits(DHT_PROT,DHT)
#define DHT_Low    GPIO_ResetBits(DHT_PROT,DHT)


void DHT11_User_Config(void);
void DHT_Mode(uint16_t mode);
void Send_Start(void);
void DHT_Send_Response(void);
uint8_t DHT11_Read_Byte(void);
void DHT11_Read_Data(uint8_t *humiH,uint8_t *humiL,uint8_t *tempH,uint8_t *tempL);
#endif


