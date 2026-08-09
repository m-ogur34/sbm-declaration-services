package tr.com.allianz.ysv.services.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope; deliberately not {@code Page} so that the JSON contract does
 * not change with Spring Data internals.
 */
public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              long totalElements,
                              int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
