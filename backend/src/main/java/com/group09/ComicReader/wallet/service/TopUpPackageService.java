package com.group09.ComicReader.wallet.service;

import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.wallet.dto.TopUpPackageRequest;
import com.group09.ComicReader.wallet.dto.TopUpPackageResponse;
import com.group09.ComicReader.wallet.entity.TopUpPackageEntity;
import com.group09.ComicReader.wallet.repository.TopUpPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopUpPackageService {

    private final TopUpPackageRepository packageRepository;

    public TopUpPackageService(TopUpPackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public List<TopUpPackageResponse> getActivePackages() {
        return packageRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TopUpPackageResponse> getAllPackages() {
        return packageRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TopUpPackageResponse createPackage(TopUpPackageRequest request) {
        TopUpPackageEntity entity = new TopUpPackageEntity();
        applyRequest(entity, request);
        return toResponse(packageRepository.save(entity));
    }

    @Transactional
    public TopUpPackageResponse updatePackage(Long id, TopUpPackageRequest request) {
        TopUpPackageEntity entity = packageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Package not found: " + id));
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(packageRepository.save(entity));
    }

    @Transactional
    public TopUpPackageResponse disablePackage(Long id) {
        TopUpPackageEntity entity = packageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Package not found: " + id));
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(packageRepository.save(entity));
    }

    @Transactional
    public TopUpPackageResponse enablePackage(Long id) {
        TopUpPackageEntity entity = packageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Package not found: " + id));
        entity.setActive(true);
        entity.setUpdatedAt(LocalDateTime.now());
        return toResponse(packageRepository.save(entity));
    }

    private void applyRequest(TopUpPackageEntity entity, TopUpPackageRequest request) {
        entity.setName(request.getName());
        entity.setCoins(request.getCoins());
        entity.setPriceLabel(request.getPriceLabel());
        entity.setBonusLabel(request.getBonusLabel() != null ? request.getBonusLabel() : "");
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    }

    private TopUpPackageResponse toResponse(TopUpPackageEntity entity) {
        TopUpPackageResponse r = new TopUpPackageResponse();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setCoins(entity.getCoins());
        r.setPriceLabel(entity.getPriceLabel());
        r.setBonusLabel(entity.getBonusLabel());
        r.setActive(entity.getActive());
        r.setSortOrder(entity.getSortOrder());
        return r;
    }
}
