#ifndef __USART2_H
#define	__USART2_H

#include "stdint.h"

void usart2_Init(unsigned int bound);
void USART2_IRQHandler(void);
uint8_t usart_get_su_RXflag(void);
void vioce_analysis(uint8_t info);
void usart_send2su_array(uint8_t *array, uint16_t len);
void usart_send2su_bit(uint8_t data);

#endif


