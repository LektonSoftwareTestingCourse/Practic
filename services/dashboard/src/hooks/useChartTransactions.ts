import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import fetchApi from "../api/client";
import { Transaction, SearchResponse } from "../types/index.ts";
import { useToastContext } from '../contexts/ToastContext.ts';

interface UseChartTransactionsOptions {
    refreshIntervalMs?: number;
}

const MAX_LIMIT = 500;
const SAFETY_CAP = 30_000;

export function useChartTransactions(
    liveTransactions: Transaction[],
    options: UseChartTransactionsOptions = {}
) {
    const { refreshIntervalMs = 60_000 } = options;

    const [restTransactions, setRestTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const { addToast } = useToastContext();

    const addToastRef = useRef(addToast);
    useEffect(() => { addToastRef.current = addToast; }, [addToast]);

    const refresh = useCallback(() => {
        setRefreshKey(k => k + 1);
    }, []);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError(null);

        const dateFromStr = new Date().toISOString().slice(0, 10);

        const loadAll = async () => {
            const all: Transaction[] = [];
            let offset = 0;
            let total = Infinity;

            while (offset < total && all.length < SAFETY_CAP) {
                const params = new URLSearchParams();
                params.append('dateFrom', dateFromStr);
                params.append('limit', MAX_LIMIT.toString());
                params.append('offset', offset.toString());

                const data = await fetchApi<SearchResponse>(
                    `/api/transactions/search?${params.toString()}`,
                    { onError: (msg) => addToastRef.current(msg, 'ERROR') }
                );

                if (cancelled) return;

                total = data.total;
                all.push(...data.transactions);

                if (data.transactions.length < MAX_LIMIT) break;
                offset += MAX_LIMIT;
            }

            if (!cancelled) setRestTransactions(all);
        };

        loadAll()
            .catch((err) => {
                if (cancelled) return;
                setError(err instanceof Error ? err.message : String(err));
                setRestTransactions([]);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    }, [refreshKey]);

    useEffect(() => {
        if (!refreshIntervalMs || refreshIntervalMs <= 0) return;
        const id = setInterval(refresh, refreshIntervalMs);
        return () => clearInterval(id);
    }, [refreshIntervalMs, refresh]);

    const transactions = useMemo(() => {
        const seen = new Set<string>();
        const result: Transaction[] = [];

        const addUnique = (tx: Transaction) => {
            if (seen.has(tx.id)) return;
            seen.add(tx.id);
            result.push(tx);
        };

        for (const tx of liveTransactions) addUnique(tx);
        for (const tx of restTransactions) addUnique(tx);

        return result;
    }, [restTransactions, liveTransactions]);

    return {
        transactions,
        loading,
        error,
        refresh,
    };
}

export default useChartTransactions;
