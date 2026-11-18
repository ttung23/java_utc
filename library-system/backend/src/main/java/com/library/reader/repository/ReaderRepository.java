package com.library.reader.repository;

import com.library.reader.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long>, JpaSpecificationExecutor<Reader> {

    Optional<Reader> findByUserId(Long userId);

    Optional<Reader> findByIdAndDeletedAtIsNull(Long id);

    Optional<Reader> findByUserIdAndDeletedAtIsNull(Long userId);

    long countByReaderType_IdAndDeletedAtIsNull(Long readerTypeId);
}
