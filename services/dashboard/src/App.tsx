import TransactionLineChart from "./components/TransactionLineChart.tsx";
import { Header } from "./components/Header.tsx";
import TransactionPieChart from "./components/TransactionPieChart.tsx";
import { TransactionTable } from "./components/TransactionTable.tsx";
import { useLiveStats } from "./hooks/useLiveStats.ts";
import { useWebSocket } from "./hooks/useWebSocket.ts";
import useTransactions from './hooks/useTransactions.ts';
import { useMemo } from 'react';
import { TransactionMap } from "./components/TransactionsMap.tsx";
import TransactionHistogram from './components/TransactionHistogram.tsx';
import { ToastContainer } from "./components/ToastContainer.tsx";
import DeclineReasonChart from './components/DeclineReasonChart.tsx';
import useChartTransactions from "./hooks/useChartTransactions.ts";
import { Routes, Route } from 'react-router-dom';
import TransactionDetailsPage from "./components/TransactionDetailsPage.tsx";
import ResponseTimeHistogram from "./components/ResponseTimeHistogram.tsx";
import { ErrorBoundary } from "./components/ErrorBoundary.tsx";

function App() {
    const { liveTransactions, isConnected } = useWebSocket();
    const {
        transactions,
        isFiltered,
        currentFilter,
        loading,
        error,
        applyFilter,
        goToPage,
        changePageSize,
        pagination,
    } = useTransactions();
    const { stats, loading: liveLoading, error: liveError } = useLiveStats(liveTransactions);
    const {
        transactions: chartTransactions,
        loading: chartLoading,
        error: chartError,
    } = useChartTransactions(liveTransactions, { refreshIntervalMs: 1_200_000 });

    const pageTransactions = useMemo(() => {
        const base = transactions || [];

        if (pagination.currentPage === 0 && !isFiltered) {
            const all = [...liveTransactions, ...base];
            const seen = new Set();
            const unique = all.filter(tx => {
                if (seen.has(tx.id)) return false;
                seen.add(tx.id);
                return true;
            });
            return unique.slice(0, pagination.pageSize);
        }

        return base;
    }, [transactions, liveTransactions, pagination.currentPage, isFiltered, pagination.pageSize]);

    return (
        <div className="bg-zinc-200 dark:bg-sage-500 min-h-screen flex flex-col items-center justify-items-stretch">
            <Routes>
                <Route path="/" element={
                    <>
                        <ErrorBoundary name="Header">
                            <Header
                                stats={stats}
                                loading={liveLoading}
                                error={liveError}
                                isConnected={isConnected}
                            />
                        </ErrorBoundary>

                        <main className="w-[95%] md:w-[90%] xl:w-3/4 flex-grow grid grid-cols-1 md:grid-cols-2 gap-4 my-5">

                            <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                    Распределение сумм транзакций
                                </h2>
                                <div className="flex-grow min-h-[300px]">
                                    <ErrorBoundary name="Распределение сумм">
                                        <TransactionHistogram
                                            transactions={chartTransactions}
                                            loading={chartLoading}
                                            error={chartError}
                                        />
                                    </ErrorBoundary>
                                </div>
                            </div>

                            <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                    Статистика одобрения транзакций
                                </h2>
                                <div className="flex-grow min-h-[300px]">
                                    <ErrorBoundary name="Статистика одобрения">
                                        <TransactionPieChart
                                            transactions={chartTransactions}
                                            loading={chartLoading}
                                            error={chartError}
                                        />
                                    </ErrorBoundary>
                                </div>
                            </div>

                            <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                    Причины отклонения транзакций
                                </h2>
                                <div className="flex-grow min-h-[300px]">
                                    <ErrorBoundary name="Причины отклонения">
                                        <DeclineReasonChart
                                            transactions={chartTransactions}
                                            loading={chartLoading}
                                            error={chartError}
                                        />
                                    </ErrorBoundary>
                                </div>
                            </div>

                            <div className="bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                    Транзакции за последний час
                                </h2>
                                <div className="flex-grow min-h-[300px]">
                                    <ErrorBoundary name="Транзакции за час">
                                        <TransactionLineChart
                                            transactions={chartTransactions}
                                            loading={chartLoading}
                                            error={chartError}
                                        />
                                    </ErrorBoundary>
                                </div>
                            </div>

                            <div className="col-span-1 md:col-span-2 flex justify-center p-4">
                                <div className="w-full max-w-[800px] bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                    <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                        Статистика времени ответа транзакций
                                    </h2>
                                    <div className="flex-grow min-h-[300px]">
                                        <ErrorBoundary name="Время ответа">
                                            <ResponseTimeHistogram
                                                transactions={chartTransactions}
                                                loading={chartLoading}
                                                error={chartError}
                                            />
                                        </ErrorBoundary>
                                    </div>
                                </div>
                            </div>

                            <div className="col-span-1 md:col-span-2 pt-6 place-content-center">
                                <ErrorBoundary name="Таблица транзакций">
                                    <TransactionTable
                                        transactions={pageTransactions}
                                        isFiltered={isFiltered}
                                        currentFilter={currentFilter}
                                        error={error}
                                        loading={loading}
                                        search={applyFilter}
                                        pagination={pagination}
                                        onPageChange={goToPage}
                                        onPageSizeChange={changePageSize}
                                    />
                                </ErrorBoundary>
                            </div>

                            <div className="col-span-1 md:col-span-2 pt-6 bg-zinc-300 dark:bg-sage-400 rounded-xl shadow-lg flex flex-col p-4">
                                <h2 className="text-xl font-bold text-center drop-shadow-lg dark:text-sage-50 mb-4 font-mono">
                                    Карта транзакций на текущей странице
                                </h2>
                                <div className="flex-grow min-h-[550px]">
                                    <ErrorBoundary name="Карта транзакций">
                                        <TransactionMap transactions={pageTransactions} />
                                    </ErrorBoundary>
                                </div>
                            </div>
                        </main>

                        <footer className="m-4 w-5/6 py-4 grid grid-cols-1 md:grid-cols-3 gap-4 dark:text-sage-50">
                            <h1 className="p-4 text-center text-xl font-mono font-bold">Практика</h1>
                            <h1 className="p-4 text-center text-xl font-mono font-bold">СМП - Система медленных платежей</h1>
                            <h1 className="p-4 text-center text-xl font-mono font-bold">2026</h1>
                        </footer>

                        <ToastContainer />
                    </>
                } />

                <Route path="/transaction/:id" element={
                    <ErrorBoundary name="Детали транзакции">
                        <TransactionDetailsPage />
                    </ErrorBoundary>
                } />
            </Routes>
        </div>
    );
}

export default App;
