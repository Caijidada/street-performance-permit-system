package com.pdk.module.venue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.venue.entity.Venue;

import java.util.List;

public interface VenueService extends IService<Venue> {

    List<Venue> listMapVenues();

    Venue getVenueDetail(Long id);

    List<Venue> findNearbyVenues(double lng, double lat, Long excludeId);
}
