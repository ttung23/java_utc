package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CopySummary {
    private long total;
    private long available;
    private long borrowed;
    private long hold;
    private long lost;
}
