#include "stm32f10x.h"                  // Device header

#include "dht11.h"
#include "delay.h"

uint16_t Time = 0;

/*开时钟*/
void DHT11_User_Config(void)
{
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOG,ENABLE);
}

/*GPIO初始化*/
void DHT_Mode(uint16_t mode)//mode==1时，配置为输出模式； mode==0,配置为输入模式
{
	GPIO_InitTypeDef GPIO_InitStructure;	
	if(mode == 1)
	{
		GPIO_InitStructure.GPIO_Pin = DHT;
		GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
		GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
	}	
	else
	{
		GPIO_InitStructure.GPIO_Pin = DHT;
		GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPU;//软件实现5K上拉电阻
	}	
	GPIO_Init(GPIOG,&GPIO_InitStructure);	//调用库函数初始化GPIOB	
}

/*向dht11发送启动信号，这也叫复位信号*/
void Send_Start(void)
{
	DHT_Mode(OUT);//OUT被宏定义为1，即DHT_Mode（1）：输出模式
	
	DHT_Low;//#define DHT_Low    GPIO_ResetBits(DHT_PROT,DHT)
	delay_ms(18);//拉低总线，保持18ms
	
	DHT_High;//释放总线，#define DHT_High   GPIO_SetBits(DHT_PROT,DHT)
	delay_us(40);//释放总线后要等待20~40us
}

/*传感器发送应答信号*/
//当器件检测到MCU复位信号后，拉低总线80us表示应答。再拉高总线80us开始传输数据
//这里强制认为dht11应答成功
void DHT_Send_Response(void)
{
	DHT_Mode(INT);//单片机的引脚改为接收信号的状态
	
	//检测从机低电平的时间长度，如果100us内从机不应答就退出while，继续往下执行
	while((GPIO_ReadInputDataBit(DHT_PROT,DHT) == 0) && (Time < 100))//每1us，Time自增1次   判断100us内，总线信号有没有一直保持0
	{
	Time++;
	delay_us(1);
	}
	Time = 0;//上述while循环的判断结束后，Time清零
	
	//检测从机高电平的时间长度
	while((GPIO_ReadInputDataBit(DHT_PROT,DHT) == 1) && (Time < 100))//每1us，Time自增1次   判断100us内，总线信号有没有一直保持1
	{
	Time++;
	delay_us(1);
	}
	Time = 0;//上述while循环的判断结束后，Time清零
	
}


 /*dht11读取1字节数据*/
uint8_t DHT11_Read_Byte(void)
{
	uint8_t data = 0;
	uint8_t i;
	for(i=0;i<8;i++)//按位接受8位数据
	{
		DHT_Mode(INT);
		
		//来1个50us的延时，50us的低电平是数据传输的开始
		while((GPIO_ReadInputDataBit(DHT_PROT,DHT) == 0) && (Time < 50))//每1us，Time自增1次   判断50us内，总线信号有没有一直保持0
		{
			Time++;
			delay_us(1);
		}
		Time = 0;//上述while循环的判断结束后，Time清零
		
		
		data <<= 1;
		delay_us(40);//26~28us的高电平指数据0  70us的高电平指数据1   40是自己取的介于26~28与70之间的值
		//40us后依然为高电平就是数据1
		//这里只要写1就行，程序设置什么时候写1。剩下的自动填充为0
		
		if(GPIO_ReadInputDataBit(DHT_PROT,DHT) == 1)
		{
			data |=0x01;//数据从低位往高位移
			
			//等待高电平结束
			//30us+delay_us(40)==70us。70us的高电平即是数据1
			while((GPIO_ReadInputDataBit(DHT_PROT,DHT) == 1) && (Time < 30))//每1us，Time自增1次   判断100us内，总线信号有没有一直保持0
			{
				Time++;
				delay_us(1);
			}
			Time = 0;//上述while循环的判断结束后，Time清零
		}
	}
	return data;
}


/*DATA[0]~DATA[4]:8位的湿度正数数据  8位的湿度小数数据  8位温度整数数据  8位温度小数数据 8位的校验和*/
//读取温湿度的值并校验
void DHT11_Read_Data(uint8_t *humiH,uint8_t *humiL,uint8_t *tempH,uint8_t *tempL)
{
	uint8_t i;
	uint8_t DATA[5] = {0,0,0,0,0};
	Send_Start();//初始化
	DHT_Send_Response();//dht11应答成功
	
	//dht11应答成功之后就开始传输数据了
	
	for( i=0;i<5;i++)//读取40位数据
	{
		DATA[i] = DHT11_Read_Byte();
	}
	
	delay_ms(1);
	
	if((DATA[0]+DATA[1]+DATA[2]+DATA[3] == DATA[4]))//如果校验没问题则输出测量结果
	{
		*humiH = DATA[0];
		*humiL = DATA[1];
		*tempH = DATA[2];
		*tempL = DATA[3];
	}
	
	else//否则数据清零 
	{
		for( i=0;i<5;i++)
		{
		DATA[i] = 0;
		}
	}
}
