#ifndef __USART3_H
#define __USART3_H

#define USART3_TXBUFF_SIZE   512   		   //定义串口2 发送缓冲区大小 512字节
#define USART3_RXBUFF_SIZE   512              //定义串口2 接收缓冲区大小 512字节
#include "stdio.h"	
#include "sys.h"

extern char USART3_RX_STA;            //定义一个变量 作为接收索引
extern char USART3_RX_Flag;            //定义一个变量0表示在接收数据，1表示完成
extern unsigned int Usart3_RxCounter;      //定义一个变量，记录串口2总共接收了多少字节的数据
extern char USART3_RX_BUF[USART3_RXBUFF_SIZE]; //定义一个数组，用于保存串口2接收到的数据 

void usart3_Init(unsigned int);       
void usart3_printf(char*,...) ;          
void usart3_TxData(u8 *data);
//串口3发送一个字节
void Usart3_SendByte(u8 val);
//串口3发送一个数据包
void Usart3_SendBuf(u8 *buf,u16 len);

//串口3发送一个字符串
void Usart3_SendString(u8 *str);

#endif



