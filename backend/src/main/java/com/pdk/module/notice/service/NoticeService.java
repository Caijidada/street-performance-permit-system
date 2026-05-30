package com.pdk.module.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.notice.entity.Notice;

public interface NoticeService extends IService<Notice> {

    void send(Long userId, String title, String content, int type);

    Page<Notice> myList(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long noticeId, Long userId);

    void markAllRead(Long userId);
}
