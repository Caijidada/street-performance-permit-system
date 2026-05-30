package com.pdk.module.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.module.notice.entity.Notice;
import com.pdk.module.notice.mapper.NoticeMapper;
import com.pdk.module.notice.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public void send(Long userId, String title, String content, int type) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setIsRead(0);
        save(notice);
    }

    @Override
    public Page<Notice> myList(Long userId, int page, int size) {
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getUserId, userId)
                        .orderByDesc(Notice::getCreateTime));
    }

    @Override
    public long unreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0));
    }

    @Override
    public void markRead(Long noticeId, Long userId) {
        update(new LambdaUpdateWrapper<Notice>()
                .eq(Notice::getId, noticeId)
                .eq(Notice::getUserId, userId)
                .set(Notice::getIsRead, 1));
    }

    @Override
    public void markAllRead(Long userId) {
        update(new LambdaUpdateWrapper<Notice>()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0)
                .set(Notice::getIsRead, 1));
    }
}
