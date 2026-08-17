import { useEffect, useState, useCallback } from 'react';
import fetchApi from "../api/client";
import { Transaction, Filter, SearchResponse, FilterStatus } from "../types/index.ts";
import { useSearchParams } from 'react-router-dom';
import { useToastContext } from '../contexts/ToastContext.ts'

const DEFAULT_PAGE_SIZE = 20;

function useTransactions() {
    const [searchParams, setSearchParams] = useSearchParams();

    const [transactions, setTransactions] = useState<Transaction[] | null>(null);
    const [totalElements, setTotalElements] = useState(0);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);

    const currentPage = parseInt(searchParams.get('page') || '0', 10);
    const pageSize = parseInt(searchParams.get('pageSize') || DEFAULT_PAGE_SIZE.toString(), 10);
    const {addToast} = useToastContext();

    const currentFilter: Filter = {
        status: (searchParams.get('status') as FilterStatus) || undefined,
        dateFrom: searchParams.get('dateFrom') || undefined,
        dateTo: searchParams.get('dateTo') || undefined,
        issuerId: searchParams.get('issuerId') || undefined,
        mcc: searchParams.get('mcc') || undefined,
    };

    const hasFilter = Object.values(currentFilter).some(v => v);

    const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));

    useEffect(() => {
        let cancelled = false;

        setLoading(true);
        setError(null);

        const requestParams = new URLSearchParams();
        requestParams.append('limit', pageSize.toString());
        requestParams.append('offset', (currentPage * pageSize).toString());

        if (currentFilter.status) requestParams.append('status', currentFilter.status);
        if (currentFilter.dateFrom) requestParams.append('dateFrom', currentFilter.dateFrom.slice(0, 10));
        if (currentFilter.dateTo) requestParams.append('dateTo', currentFilter.dateTo.slice(0, 10));
        if (currentFilter.issuerId) requestParams.append('issuerId', currentFilter.issuerId);
        if (currentFilter.mcc) requestParams.append('mcc', currentFilter.mcc);

        fetchApi<SearchResponse>(`/api/transactions/search?${requestParams.toString()}`, {
            onError: (message) => addToast(message, 'ERROR')
        })
            .then((data) => {
                if (cancelled) return;
                setTransactions(data.transactions);
                setTotalElements(data.total);
            })
            .catch((err) => {
                if (cancelled) return;
                setError(err.message);
                setTransactions([]);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [addToast, currentPage, pageSize, currentFilter.status, currentFilter.dateFrom, currentFilter.dateTo, currentFilter.issuerId, currentFilter.mcc]);

    const applyFilter = useCallback((filter: Filter) => {
        setSearchParams((prev) => {
            const newParams = new URLSearchParams();

            if (filter.status) newParams.set('status', filter.status);
            if (filter.dateFrom) newParams.set('dateFrom', filter.dateFrom);
            if (filter.dateTo) newParams.set('dateTo', filter.dateTo);
            if (filter.issuerId) newParams.set('issuerId', filter.issuerId);
            if (filter.mcc) newParams.set('mcc', filter.mcc);

            newParams.set('page', '0');
            newParams.set('pageSize', prev.get('pageSize') || DEFAULT_PAGE_SIZE.toString());

            return newParams;
        });
    }, [setSearchParams]);

    const goToPage = useCallback((page: number) => {
        setSearchParams((prev) => {
            const newParams = new URLSearchParams(prev);
            newParams.set('page', page.toString());
            return newParams;
        });
    }, [setSearchParams]);

    const changePageSize = useCallback((size: number) => {
        setSearchParams((prev) => {
            const newParams = new URLSearchParams(prev);
            newParams.set('pageSize', size.toString());
            newParams.set('page', '0');
            return newParams;
        });
    }, [setSearchParams]);

    return {
        transactions,
        isFiltered: hasFilter,
        currentFilter,
        error,
        loading,
        applyFilter,
        goToPage,
        changePageSize,
        pagination: {
            currentPage,
            pageSize,
            totalElements,
            totalPages,
        },
    };
}

export default useTransactions;
