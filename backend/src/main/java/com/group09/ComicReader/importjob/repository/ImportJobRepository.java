package com.group09.ComicReader.importjob.repository;

import com.group09.ComicReader.importjob.entity.ImportJobEntity;
import com.group09.ComicReader.importjob.entity.ImportJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {
    List<ImportJobEntity> findTop50ByStatusInOrderByUpdatedAtAsc(List<ImportJobStatus> statuses);
}
