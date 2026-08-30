package com.morel.greenhouse.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("greenhouse")
public class GreenhouseEntity {
    private Long id;
    private String name;
    private String location;
    private String status;
    private Double area;
    private String cropStage;
}
