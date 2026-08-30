#include "timer3.h"
#include "main.h"
#include "led.h"
#include "mqtt.h"

extern u8 pingFlag;
/********************
①能定时器时钟。
RCC_APB1PeriphClockCmd();
②  初始化定时器，配置ARR,PSC。
TIM_TimeBaseInit();
③开启定时器中断，配置NVIC。
void TIM_ITConfig();
NVIC_Init();
④使能定时器。
TIM_Cmd();
⑥编写中断服务函数。
TIMx_IRQHandler();
*****************/
void TIM3_Init(u16 arr,u16 psc)
{
	TIM_TimeBaseInitTypeDef  TIM_TimeBaseStructure;
	NVIC_InitTypeDef NVIC_InitStruct;
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM3, ENABLE); //时钟使能
	
	TIM_TimeBaseStructure.TIM_Period = arr; //设置在下一个更新事件装入活动的自动重装载寄存器周期的值	 计数到5000为500ms
	TIM_TimeBaseStructure.TIM_Prescaler =psc; //设置用来作为TIMx时钟频率除数的预分频值  10Khz的计数频率  
	TIM_TimeBaseStructure.TIM_ClockDivision = 0; //设置时钟分割:TDTS = Tck_tim
	TIM_TimeBaseStructure.TIM_CounterMode = TIM_CounterMode_Up;  //TIM向上计数模式
	TIM_TimeBaseInit(TIM3, &TIM_TimeBaseStructure); //根据TIM_TimeBaseInitStruct中指定的参数初始化TIMx的时间基数单位
 
	TIM_ITConfig(TIM3,TIM_IT_Update,ENABLE);
	
  NVIC_PriorityGroupConfig( NVIC_PriorityGroup_2);
	
	NVIC_InitStruct.NVIC_IRQChannel=TIM3_IRQn;
	NVIC_InitStruct.NVIC_IRQChannelCmd=ENABLE;
	NVIC_InitStruct.NVIC_IRQChannelPreemptionPriority=1;
	NVIC_InitStruct.NVIC_IRQChannelSubPriority=1;
  NVIC_Init(&NVIC_InitStruct);		
	
	

	TIM_Cmd(TIM3, DISABLE);  //使能TIMx外设
	
}

void TIM3_IRQHandler(void)   //TIM3中断
{
////	if(TIM_GetITStatus(TIM4,TIM_IT_Update)==SET)//溢出中断
////	{
////		usmart_dev.scan();	//执行usmart扫描	
////		TIM_SetCounter(TIM4,0);		//清空定时器的CNT
////		TIM_SetAutoreload(TIM4,100);//恢复原来的设置		    				   				     	    	
////	}				   
////	TIM_ClearITPendingBit(TIM4,TIM_IT_Update);  //清除中断标志位    
	
	if (TIM_GetITStatus(TIM3, TIM_IT_Update) != RESET) //检查指定的TIM中断发生与否:TIM 中断源 
		{
			
			//LED_GPIO_Turn();
			TIM_SetCounter(TIM3,0);		//清空定时器的CNT
			TIM_SetAutoreload(TIM3,100);//恢复原来的设置		  
			fenwei_Lamp();
			TIM_ClearITPendingBit(TIM3, TIM_IT_Update);  //清除TIMx的中断待处理位:TIM 中断源 
			
		}
}

/****************************************
	函数名：定时器4使能10s定时          *
	参  数：arr：自动重装值   0~65535   *
			psc：时钟预分频数 0~65535   *
	返回值：无                          *
*****************************************/
void TIM5_Init(u16 arr, u16 psc)
{
	TIM_TimeBaseInitTypeDef TIM_TimeBaseInitStructure;              //定义一个设置定时器的变量
	NVIC_InitTypeDef NVIC_InitStructure;                            //定义一个设置中断的变量
	
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);                 //设置中断向量分组：第2组 抢先优先级：0 1 2 3 子优先级：0 1 2 3		
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_TIM5, ENABLE);            //使能TIM4时钟	
    TIM_TimeBaseInitStructure.TIM_Period = arr; 	                //设置自动重装载值
	TIM_TimeBaseInitStructure.TIM_Prescaler = psc;                  //设置定时器预分频数
	TIM_TimeBaseInitStructure.TIM_CounterMode = TIM_CounterMode_Up; //向上计数模式
	TIM_TimeBaseInitStructure.TIM_ClockDivision = TIM_CKD_DIV1;     //1分频
	TIM_TimeBaseInit(TIM5, &TIM_TimeBaseInitStructure);             //设置TIM4
	
	TIM_ClearITPendingBit(TIM5, TIM_IT_Update);                     //清除溢出中断标志位
	TIM_ITConfig(TIM5, TIM_IT_Update, ENABLE);                      //使能TIM4溢出中断    
	TIM_Cmd(TIM5, DISABLE);                                         //先关闭TIM4                          
	
	NVIC_InitStructure.NVIC_IRQChannel = TIM5_IRQn;                 //设置TIM4中断
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 1;       //抢占优先级1
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0;              //子优先级0
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;                 //中断通道使能
	NVIC_Init(&NVIC_InitStructure);                                 //设置中断
}

/**********************************************
	函数名：定时器4中断服务函数,处理MQTT数据  *
	参  数：无                        		  *
	返回值：无                        		  *
***********************************************/
void TIM5_IRQHandler(void)
{
	if(TIM_GetITStatus(TIM5, TIM_IT_Update) != RESET)//如果TIM_IT_Update置位，表示TIM3溢出中断，进入if	
		{  
//		switch(pingFlag) 					//判断pingFlag的状态
//		{                               
//			case 0:							//如果pingFlag等于0，表示正常状态，发送Ping报文  
//					_mqtt.SendHeart();; 		//添加Ping报文到发送缓冲区  
//					break;
//			case 1:							//如果pingFlag等于1，说明上一次发送到的ping报文，没有收到服务器回复，所以1没有被清除为0，可能是连接异常，我们要启动快速ping模式
//						    //我们将定时器3设置为2s定时,快速发送Ping报文
//					_mqtt.SendHeart();		//添加Ping报文到发送缓冲区  
//					break;
//			case 2:							//如果pingFlag等于2，说明还没有收到服务器回复
//			case 3:				            //如果pingFlag等于3，说明还没有收到服务器回复
//			case 4:				            //如果pingFlag等于4，说明还没有收到服务器回复	
//					_mqtt.SendHeart();  		//添加Ping报文到发送缓冲区 
//					break;
//		}
//		pingFlag++;           		   		//pingFlag自增1，表示又发送了一次ping，期待服务器的回复
		MQTT_report();
		TIM_ClearITPendingBit(TIM5, TIM_IT_Update); //清除TIM3溢出中断标志 	
	}
}



