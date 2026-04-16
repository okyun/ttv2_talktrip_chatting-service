package org.example.talktripchattingservice.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 무한 스크롤 전용 응답 DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SliceResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {
    public static <T> SliceResponse<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new SliceResponse<>(items, nextCursor, hasNext);
    }
}

