package com.morel.greenhouse.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mysql")
@MapperScan("com.morel.greenhouse.infrastructure.persistence.mapper")
public class MybatisPlusConfiguration {
}
