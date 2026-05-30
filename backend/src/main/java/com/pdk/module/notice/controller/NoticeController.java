package com.pdk.module.notice.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdk.common.result.Result;
import com.pdk.module.notice.entity.Notice;
import com.pdk.module.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/artist/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "我的通知列表")
    @GetMapping
    public Result<Page<Notice>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noticeService.myList(userId, page, size));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(noticeService.unreadCount(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        noticeService.markRead(id, StpUtil.getLoginIdAsLong());
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        noticeService.markAllRead(StpUtil.getLoginIdAsLong());
        return Result.success();
    }
}
