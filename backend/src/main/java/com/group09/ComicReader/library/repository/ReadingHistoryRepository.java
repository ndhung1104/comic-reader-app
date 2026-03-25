package com.group09.ComicReader.library.repository;

import com.group09.ComicReader.library.entity.ReadingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEntity, Long> {

    Optional<ReadingHistoryEntity> findByUserIdAndComicId(Long userId, Long comicId);

    List<ReadingHistoryEntity> findByUserIdOrderByLastReadAtDesc(Long userId);
}
