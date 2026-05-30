package com.pdk.module.venue.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_venue")
public class Venue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer maxDecibel;
    private Integer maxAudience;
    private String allowTypes;
    private LocalTime openTimeStart;
    private LocalTime openTimeEnd;
    private String openDays;
    private String images;

    /** 点位状态：0-关闭 1-开放 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
