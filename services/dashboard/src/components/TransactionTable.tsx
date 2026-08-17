import { hidePan, convertPenniesToRubles, formatDateTime } from '../utils/format';
import { getStatusIcon } from '../utils/statusIcon';
import { Filter, PaginationMeta, Transaction } from '../types';
import { useState, useMemo, useEffect } from 'react';
import { TransactionModal } from './TransactionModal';
import { ArrowDownToLine, ChevronLeft, ChevronRight } from 'lucide-react';
import { Filters } from './Filters';
import { ISSUERS_NAMES, MCC_NAMES } from '../mockData';
import { useExportCsv } from "../hooks/useExportCsv.ts";
import { useToastContext } from '../contexts/ToastContext.ts'
import { Listbox } from '@headlessui/react';

type TransactionTableProps = {
    transactions: Transaction[],
    isFiltered: boolean,
    currentFilter: Filter,
    error: string | null,
    loading: boolean,
    search: (filter: Filter) => void,
    pagination: PaginationMeta,
    onPageChange: (page: number) => void,
    onPageSizeChange: (size: number) => void,
};

export function TransactionTable({
     transactions,
     isFiltered,
     currentFilter,
     error,
     loading,
     search,
     pagination,
     onPageChange,
     onPageSizeChange
}: TransactionTableProps){
    const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
    const { exportError, exportSuccess, handleExportCsv } = useExportCsv(currentFilter);
    const { addToast } = useToastContext();

    const sortedTransactions = useMemo(() => {
        return [...transactions].sort((a, b) => {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
    }, [transactions]);

    useEffect(() => {
        if (exportError) {
            addToast('Не удалось скачать файл', 'ERROR');
        } else if (exportSuccess) {
            addToast('Файл успешно экспортирован', 'SUCCESS');
        }
    }, [exportError, exportSuccess, addToast]);

    const showNavigation = !loading && !error && pagination.totalElements > 0;

    const pageNumbers = useMemo(() => {
        const pages: (number | string)[] = [];
        const maxVisible = 5;

        if (pagination.totalPages <= maxVisible) {
            for (let i = 0; i < pagination.totalPages; i++) {
                pages.push(i);
            }
        } else {
            const start = Math.max(0, pagination.currentPage - 2);
            const end = Math.min(pagination.totalPages - 1, start + maxVisible - 1);

            if (start > 0) pages.push(0, '...');
            for (let i = start; i <= end; i++) pages.push(i);
            if (end < pagination.totalPages - 1) pages.push('...', pagination.totalPages - 1);
        }

        return pages;
    }, [pagination.currentPage, pagination.totalPages]);

    return (
        <div className="font-mono w-full">
            <Filters issuers={ISSUERS_NAMES} mccNames={MCC_NAMES} onSearch={search}/>

            <div className="flex flex-col sm:flex-row items-center justify-between mt-5 mb-3">
                <h2 className="text-2xl font-bold text-center drop-shadow-lg dark:text-sage-50">
                    {isFiltered ? 'Результаты поиска' : 'Последние транзакции'}
                </h2>

                {!loading && !error && sortedTransactions.length > 0 && (
                    <div className="flex items-center gap-3">
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-bold dark:text-sage-50">Записей:</label>
                            <div className="relative w-12">
                                <Listbox
                                    value={pagination.pageSize}
                                    onChange={(size) => onPageSizeChange(size)}
                                >
                                    <Listbox.Button className="border border-zinc-300 dark:border-sage-200 rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer p-2 w-full text-left text-sm">
                                        {pagination.pageSize}
                                    </Listbox.Button>
                                    <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800 p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                                        <Listbox.Option value={10} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            10
                                        </Listbox.Option>
                                        <Listbox.Option value={20} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            20
                                        </Listbox.Option>
                                        <Listbox.Option value={50} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            50
                                        </Listbox.Option>
                                        <Listbox.Option value={100} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            100
                                        </Listbox.Option>
                                    </Listbox.Options>
                                </Listbox>
                            </div>
                        </div>
                        <button
                            className="
                                px-3 md:px-3 py-[5px]  text-base md:text-lg rounded-xl bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 font-semibold cursor-pointer
                                hover:bg-emerald-500 hover:text-zinc-200 transition-none hover:transition-colors hover:duration-200
                                dark:hover:bg-sage-100 dark:hover:text-sage-500
                                flex items-center gap-1
                                disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-emerald-400 disabled:hover:text-white
                                disabled:hover:bg-sage-200 disabled:hover:text-sage-50"
                            onClick={handleExportCsv}
                            disabled={sortedTransactions.length === 0} >
                            <ArrowDownToLine size={16} strokeWidth={3} className="inline-block"/>
                            CSV
                        </button>
                    </div>
                )}
            </div>


            {error &&
                <div className="text-center py-8 text-red-500">
                    Ошибка загрузки транзакций: {error}
                </div>
            }

            {loading &&
                <div className="text-center py-8 text-gray-500 dark:text-sage-50">
                    Загрузка транзакций...
                </div>
            }

            {!loading && !error && sortedTransactions.length === 0 &&
                <div className="text-center py-8 dark:text-sage-50">Транзакций не найдено</div>
            }

            {!loading && !error && sortedTransactions.length > 0 &&
                <>
                    <div className="rounded-xl border-2 border-emerald-600 dark:border-sage-200 shadow-lg mb-5 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full min-w-[900px] text-sm">
                                <thead>
                                <tr className="border-b-2 border-emerald-600 dark:border-sage-200 text-center font-semibold dark:text-sage-50">
                                    <th className="px-3 md:px-6 py-2 md:py-4">Дата, Время</th>
                                    <th className="px-3 md:px-6 py-2 md:py-4">PAN</th>
                                    <th className="px-3 md:px-6 py-2 md:py-4 text-right">Сумма</th>
                                    <th className="px-3 md:px-6 py-2 md:py-4">Мерчант</th>
                                    <th className="px-3 md:px-6 py-2 md:py-4">Статус</th>
                                </tr>
                                </thead>
                                <tbody>
                                {sortedTransactions.map((transaction) => {
                                    const statusIconData = getStatusIcon(transaction.status);

                                    return (
                                        <tr
                                            key={transaction.id}
                                            className="text-center cursor-pointer hover:bg-emerald-50 dark:hover:bg-sage-400 transition-colors border-b border-gray-100 dark:border-sage-400 last:border-0 dark:text-sage-50"
                                            onClick={() => setSelectedTx(transaction)}
                                        >
                                            <td className="px-3 md:px-6 py-2 md:py-4">{formatDateTime(transaction.createdAt)}</td>
                                            <td className="px-3 md:px-6 py-2 md:py-4">{hidePan(transaction.pan)}</td>
                                            <td className="px-3 md:px-6 py-2 md:py-4 text-right">{convertPenniesToRubles(transaction.amount)}</td>
                                            <td className="px-3 md:px-6 py-2 md:py-4">{transaction.merchantId}</td>
                                            <td className="px-3 md:px-6 py-2 md:py-4">
                                                <div className="flex justify-center items-center gap-2">
                                                    <statusIconData.icon
                                                        className={statusIconData.color}
                                                        size={statusIconData.size}
                                                        aria-hidden="true"
                                                    />
                                                    <span className="sr-only">{statusIconData.label}</span>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {showNavigation && (
                        <div className="flex items-center justify-center gap-2 mb-4">
                            <button
                                onClick={() => onPageChange(0)}
                                disabled={pagination.currentPage === 0}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                «
                            </button>
                            <button
                                onClick={() => onPageChange(pagination.currentPage - 1)}
                                disabled={pagination.currentPage === 0}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                <ChevronLeft size={16} />
                            </button>

                            {pageNumbers.map((page, idx) => (
                                typeof page === 'string' ? (
                                    <span key={`ellipsis-${idx}`} className="px-2 dark:text-sage-50">...</span>
                                ) : (
                                    <button
                                        key={page}
                                        onClick={() => onPageChange(page)}
                                        className={`px-3 py-1 rounded-lg transition-colors ${
                                            page === pagination.currentPage
                                                ? 'bg-emerald-600 dark:bg-sage-100 text-white dark:text-sage-500 font-bold'
                                                : 'bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 hover:bg-emerald-500 dark:hover:bg-sage-100'
                                        }`}
                                    >
                                        {page + 1}
                                    </button>
                                )
                            ))}

                            <button
                                onClick={() => onPageChange(pagination.currentPage + 1)}
                                disabled={pagination.currentPage >= pagination.totalPages - 1}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                <ChevronRight size={16} />
                            </button>
                            <button
                                onClick={() => onPageChange(pagination.totalPages - 1)}
                                disabled={pagination.currentPage >= pagination.totalPages - 1}
                                className="px-3 py-1 rounded-lg bg-emerald-400 dark:bg-sage-200 dark:text-sage-400 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-emerald-500 dark:hover:bg-sage-100 transition-colors"
                            >
                                »
                            </button>
                        </div>
                    )}
                </>
            }
            {selectedTx && (
                <TransactionModal
                    transaction={selectedTx}
                    onClose={() => setSelectedTx(null)}
                />
            )}
        </div>
    );
}
