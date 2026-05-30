package com.pdk.module.venue.controller;

import com.pdk.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 高德地图 REST API 代理
 * 解决前端直接调用 restapi.amap.com 的 CORS 跨域问题
 */
@Slf4j
@RestController
@RequestMapping("/api/amap")
public class AmapProxyController {

    @Value("${pdk.amap-web-key}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 地址转坐标（地理编码） */
    @GetMapping("/geocode")
    public Result<Object> geocode(
            @RequestParam String address,
            @RequestParam(required = false, defaultValue = "") String city) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                .queryParam("address", address)
                .queryParam("city", city)
                .queryParam("key", amapKey)
                .queryParam("output", "JSON")
                .build().toUriString();
        try {
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.error("高德地理编码失败: {}", e.getMessage());
            return Result.error(500, "地理编码请求失败");
        }
    }

    /** 坐标转地址（逆地理编码） */
    @GetMapping("/regeo")
    public Result<Object> regeo(@RequestParam String location) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://restapi.amap.com/v3/geocode/regeo")
                .queryParam("location", location)
                .queryParam("key", amapKey)
                .queryParam("extensions", "base")
                .queryParam("output", "JSON")
                .build().toUriString();
        try {
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.error("高德逆地理编码失败: {}", e.getMessage());
            return Result.error(500, "逆地理编码请求失败");
        }
    }

    /** POI 关键词搜索 */
    @GetMapping("/place/search")
    public Result<Object> placeSearch(
            @RequestParam String keywords,
            @RequestParam(required = false, defaultValue = "") String city) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://restapi.amap.com/v3/place/text")
                .queryParam("keywords", keywords)
                .queryParam("city", city)
                .queryParam("citylimit", !city.isEmpty())
                .queryParam("offset", 1)
                .queryParam("page", 1)
                .queryParam("key", amapKey)
                .queryParam("output", "JSON")
                .build().toUriString();
        try {
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            log.error("高德 POI 搜索失败: {}", e.getMessage());
            return Result.error(500, "POI搜索请求失败");
        }
    }
}
