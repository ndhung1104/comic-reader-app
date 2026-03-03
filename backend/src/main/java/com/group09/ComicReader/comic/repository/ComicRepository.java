package com.group09.ComicReader.comic.repository;

import com.group09.ComicReader.comic.entity.ComicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComicRepository extends JpaRepository<ComicEntity, Long> {
}

