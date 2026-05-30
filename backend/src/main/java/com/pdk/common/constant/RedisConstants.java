package com.pdk.common.constant;

public interface RedisConstants {
    /** 点位详情缓存，TTL 30分钟 */
    String VENUE_DETAIL = "venue:detail:";

    /** 报备分布式锁：lock:report:{venueId}:{date}:{timeSlot} */
    String REPORT_LOCK = "lock:report:";

    /** Token 黑名单 */
    String TOKEN_BLACKLIST = "token:blacklist:";

    /** 验证码，TTL 5分钟 */
    String CAPTCHA = "captcha:";

    long VENUE_DETAIL_TTL = 1800L;
    long REPORT_LOCK_TTL = 10L;
    long CAPTCHA_TTL = 300L;
}
