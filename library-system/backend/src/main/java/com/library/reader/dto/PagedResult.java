package com.library.reader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class PagedResult<T> {
    private List<T> items;
    private int page;
    private int limit;
    private long total;

    public Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page);
        meta.put("limit", limit);
        meta.put("total", total);
        return meta;
    }
}
