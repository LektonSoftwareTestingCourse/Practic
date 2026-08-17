import { useState, useCallback } from 'react';
import { Filter } from '../types';
import { fetchApiBlob } from '../api/client';
import { hidePan } from '../utils/format';

const buildFilterQuery = (filter: Filter): string => {
    const params = new URLSearchParams();
    if (filter.status) params.append('status', filter.status);
    if (filter.dateFrom) params.append('dateFrom', filter.dateFrom.slice(0, 10));
    if (filter.dateTo) params.append('dateTo', filter.dateTo.slice(0, 10));
    if (filter.issuerId) params.append('issuerId', filter.issuerId);
    if (filter.mcc) params.append('mcc', filter.mcc);
    const qs = params.toString();
    return qs ? `?${qs}` : '';
};

const maskPanInCsv = (csvText: string): string => {
    const lines = csvText.split('\n');
    if (lines.length === 0) return csvText;

    const headers = lines[0].split(',');
    const panIndex = headers.findIndex(h => h.trim().toLowerCase() === 'pan');

    if (panIndex === -1) return csvText;

    const maskedLines = lines.map((line, index) => {
        if (index === 0) return line;

        const columns = line.split(',');
        if (columns.length > panIndex && columns[panIndex]) {
            columns[panIndex] = hidePan(columns[panIndex].trim());
        }
        return columns.join(',');
    });

    return maskedLines.join('\n');
};

export function useExportCsv(currentFilter: Filter) {
    const [exporting, setExporting] = useState(false);
    const [exportError, setExportError] = useState<string | null>(null);
    const [exportSuccess, setExportSuccess] = useState(false);

    const handleExportCsv = useCallback(async () => {
        setExportError(null);
        setExportSuccess(false);
        setExporting(true);
        try {
            const url = `/api/transactions/export${buildFilterQuery(currentFilter)}`;
            const blob = await fetchApiBlob(url);

            const csvText = await blob.text();
            const maskedCsvText = maskPanInCsv(csvText);
            const maskedBlob = new Blob([maskedCsvText], { type: 'text/csv;charset=utf-8;' });

            const now = new Date();
            const dateStr = now.toISOString().slice(0, 10);
            const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '-');
            const filename = `transactions_${dateStr}_${timeStr}.csv`;

            const link = document.createElement('a');
            const objectUrl = URL.createObjectURL(maskedBlob);
            link.href = objectUrl;
            link.download = filename;
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(objectUrl);

            setExportSuccess(true);
        } catch (e) {
            setExportError(e instanceof Error ? e.message : 'Не удалось выгрузить CSV');
        } finally {
            setExporting(false);
        }
    }, [currentFilter]);

    return {
        exporting,
        exportError,
        exportSuccess,
        handleExportCsv,
    };
}
