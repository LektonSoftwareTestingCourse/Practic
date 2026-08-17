import { useEffect, useState } from 'react';
import fetchApi from "../api/client";
import { DashboardStats } from "../types/index.ts";
import { useToastContext } from '../contexts/ToastContext.ts'

function useStats() {
    const [transactionStats, setTransactionStats] = useState<DashboardStats | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(true);
    const {addToast} = useToastContext();

    useEffect(() => {
        fetchApi<DashboardStats>("/api/dashboard/stats", {
            onError: (message) => addToast(message, 'ERROR')
        })
            .then((data) => {
                setTransactionStats(data);
                setLoading(false);
            })
            .catch((error) => {
                setError(error.message);
                setLoading(false)});
    }, [addToast]);
    return {transactionStats, error, loading}
}

export default useStats;
