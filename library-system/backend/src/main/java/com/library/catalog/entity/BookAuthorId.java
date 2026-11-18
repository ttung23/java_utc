package com.library.catalog.entity;

import com.library.common.enums.BookAuthorRole;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BookAuthorId implements Serializable {
    private Long bookId;
    private Long authorId;
    private BookAuthorRole role;
}
