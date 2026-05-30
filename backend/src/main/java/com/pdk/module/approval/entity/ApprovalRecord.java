package com.pdk.module.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long approverId;

    /** 动作：1-一审通过 2-一审驳回 3-终审通过 4-终审驳回 */
    private Integer action;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
