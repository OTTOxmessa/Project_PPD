package com.example.portfolio.service;

import com.example.portfolio.domain.entity.Asset;

import java.util.List;

public interface WatchlistService {

    List<Asset> getWatchlist(Long userId);

    Asset add(Long userId, Long assetId);

    void remove(Long userId, Long assetId);
}
