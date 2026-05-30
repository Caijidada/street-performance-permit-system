package com.pdk.module.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdk.module.venue.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

    /**
     * 查询目标点位 100 米范围内的其他点位（Haversine 公式）
     */
    @Select("""
        SELECT id, name, longitude, latitude,
          (6371000 * ACOS(
            COS(RADIANS(#{lat})) * COS(RADIANS(latitude)) *
            COS(RADIANS(longitude) - RADIANS(#{lng})) +
            SIN(RADIANS(#{lat})) * SIN(RADIANS(latitude))
          )) AS distance
        FROM t_venue
        WHERE id != #{excludeId} AND status = 1
        HAVING distance <= 100
        ORDER BY distance
        """)
    List<Venue> findNearbyVenues(@Param("lng") double lng, @Param("lat") double lat,
                                  @Param("excludeId") Long excludeId);
}
