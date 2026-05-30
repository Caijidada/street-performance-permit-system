package com.pdk.module.statistics.service;

import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getOverview();
    Object getHeatmapData();
    Object getVenueRate();
    Object getArtistRank();
    Object getPerformTypeDistribution();
}
