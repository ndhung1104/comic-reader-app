package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.TopUpPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopUpPackageRepository extends JpaRepository<TopUpPackageEntity, Long> {

    List<TopUpPackageEntity> findByActiveTrueOrderBySortOrderAsc();

    List<TopUpPackageEntity> findAllByOrderBySortOrderAsc();

    Optional<TopUpPackageEntity> findByIdAndActiveTrue(Long id);
}
