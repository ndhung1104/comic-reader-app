package com.group09.ComicReader.wallet.repository;

import com.group09.ComicReader.wallet.entity.TopUpPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopUpPackageRepository extends JpaRepository<TopUpPackageEntity, Long> {

    List<TopUpPackageEntity> findByActiveTrueOrderBySortOrderAsc();

    List<TopUpPackageEntity> findAllByOrderBySortOrderAsc();
}
