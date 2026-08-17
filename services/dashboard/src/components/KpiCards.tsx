import {convertPenniesToRubles} from "../utils/format.ts";

export type KpiCardsStats = {
    totalTransactions: number,
    approvalRate: number,
    totalAmount: number,
    avgProcessingTimeMs: number,
};

type KpiCardsProps = {
    stats: KpiCardsStats,
}

export function KpiCards( { stats }: KpiCardsProps ) {
    const kpiCards = [
        { label: 'Всего ТХ', value: stats.totalTransactions },
        { label: 'Одобрено', value: stats.approvalRate, unit: '%' },
        { label: 'Общая сумма', value: convertPenniesToRubles(stats.totalAmount) },
        { label: 'Среднее время', value: stats.avgProcessingTimeMs, unit: 'ms' },
    ];

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2 md:gap-4 w-[95%] md:w-[90%] xl:w-3/4 mx-auto my-5">
            {kpiCards.map((kpiCard) => (
                <div
                    key={kpiCard.label}
                    className="bg-emerald-400 rounded-xl p-3 md:p-4 text-base md:text-xl shadow-lg text-center dark:bg-sage-300 dark:text-sage-50">
                    <p>{kpiCard.value} {kpiCard.unit}</p>
                    <p>{kpiCard.label}</p>
                </div>
            ))}
        </div>
    );
}
