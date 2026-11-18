package com.library.reader.repository;

import com.library.reader.entity.ReaderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReaderTypeRepository extends JpaRepository<ReaderType, Long> {

    Optional<ReaderType> findByCode(String code);

    Optional<ReaderType> findByIdAndDeletedAtIsNull(Long id);

    List<ReaderType> findByDeletedAtIsNullOrderByCodeAsc();

    List<ReaderType> findByDeletedAtIsNullAndActiveTrueOrderByCodeAsc();

    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}
