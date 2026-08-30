#ifndef __WIFI_H
#define __WIFI_H

#include "sys.h"
/*连接AP宏定义*/
#define SSID "your_wifi_name"  　//wifi名称
#define PWD  "your_wifi_password"     //wifi密码

/*连接服务器宏定义*/
#define TCP "TCP"
#define UDP "UDP"
#define IP  "your_broker_address"   //wifi的ipv4地址
#define PORT 1883  //gai

#define Net_SendString(str) Usart_SendString(str)
#define Net_SendBuf(buf,len) Usart_SendBuf(buf,len)

u8 FindStr(char* dest,char* src,u16 retry_nms);

typedef struct
{
	u8 *rxbuf;u16 rxlen;
	u8 *txbuf;u16 txlen;
	
	u8 (*Check)(void);
	u8 (*Init)(u8 *prx,u16 rxlen,u8 *ptx,u16 txlen);
	void (*Restore)(void);
	u8 (*ConnectAP)(char *ssid,char *pswd);
	u8 (*ConnectServer)(char* mode,char *ip,u16 port);
	u8 (*DisconnectServer)(void);
	u8 (*OpenTransmission)(void);
	void (*CloseTransmission)(void);		
	void (*SendString)(char *str);
	void (*SendBuf)(u8 *buf,u16 len);
}_typdef_net;

extern _typdef_net _net;

#endif

/*********************************************END OF FILE********************************************/

