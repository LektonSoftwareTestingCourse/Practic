package com.processing.transactionlogger.specification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Реализация {@link Pageable} на основе offset/limit.
 * Spring Data по умолчанию работает через номер страницы — этот класс
 * позволяет использовать прямое смещение, как требует API поиска транзакций.
 */
@RequiredArgsConstructor
public class OffsetBasedPageRequest implements Pageable {
    private final int offset;
    private final int limit;
    private final Sort sort;

    public OffsetBasedPageRequest(int offset, int limit) {
        this(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageRequest(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return new OffsetBasedPageRequest(Math.max(0, offset - limit), limit);
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageRequest(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetBasedPageRequest(pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
