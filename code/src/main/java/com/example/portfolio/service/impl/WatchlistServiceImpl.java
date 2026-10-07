package com.example.portfolio.service.impl;

import com.example.portfolio.domain.entity.Asset;
import com.example.portfolio.domain.entity.User;
import com.example.portfolio.exception.ResourceNotFoundException;
import com.example.portfolio.repository.AssetRepository;
import com.example.portfolio.repository.UserRepository;
import com.example.portfolio.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Asset> getWatchlist(Long userId) {
        return new ArrayList<>(getUser(userId).getWatchlist());
    }

    @Override
    @Transactional
    public Asset add(Long userId, Long assetId) {
        User user = getUser(userId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        boolean alreadyAdded = user.getWatchlist().stream().anyMatch(a -> a.getId().equals(assetId));
        if (!alreadyAdded) {
            user.getWatchlist().add(asset); // dirty checking บันทึกลงตาราง user_watchlist ให้เอง
        }
        return asset;
    }

    @Override
    @Transactional
    public void remove(Long userId, Long assetId) {
        getUser(userId).getWatchlist().removeIf(a -> a.getId().equals(assetId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
