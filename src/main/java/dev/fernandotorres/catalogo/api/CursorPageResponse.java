package dev.fernandotorres.catalogo.api;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        int size,
        boolean hasNext,
        String nextCursor
) {
}
