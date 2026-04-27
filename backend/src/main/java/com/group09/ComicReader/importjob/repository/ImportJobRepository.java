package com.group09.ComicReader.importjob.repository;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.common.entity.ModerationStatus;
import com.group09.ComicReader.importjob.entity.ImportJobEntity;
import com.group09.ComicReader.importjob.entity.ImportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {
    List<ImportJobEntity> findTop50ByStatusInOrderByUpdatedAtAsc(List<ImportJobStatus> statuses);

    Page<ImportJobEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    List<ImportJobEntity> findByModerationStatus(ModerationStatus status);
}
