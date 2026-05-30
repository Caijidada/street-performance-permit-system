package com.pdk.module.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.constant.RedisConstants;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.venue.entity.Venue;
import com.pdk.module.venue.mapper.VenueMapper;
import com.pdk.module.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<Venue> listMapVenues() {
        return list();
    }

    @Override
    public Venue getVenueDetail(Long id) {
        String cacheKey = RedisConstants.VENUE_DETAIL + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Venue venue) {
            return venue;
        }
        Venue venue = getById(id);
        if (venue == null) {
            throw new BusinessException(404, "点位不存在");
        }
        redisTemplate.opsForValue().set(cacheKey, venue, RedisConstants.VENUE_DETAIL_TTL, TimeUnit.SECONDS);
        return venue;
    }

    @Override
    public List<Venue> findNearbyVenues(double lng, double lat, Long excludeId) {
        return baseMapper.findNearbyVenues(lng, lat, excludeId);
    }
}
