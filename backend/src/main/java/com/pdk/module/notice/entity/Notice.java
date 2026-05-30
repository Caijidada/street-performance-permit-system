package com.pdk.module.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 类型：1-审批通知 2-信用通知 3-系统通知 */
    private Integer type;

    /** 0-未读 1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
