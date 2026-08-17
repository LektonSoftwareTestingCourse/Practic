import { Transaction } from '../types/index.ts'
import { BarChart, XAxis, YAxis, Tooltip, CartesianGrid, Bar, ResponsiveContainer, TooltipProps } from 'recharts'
import { ValueType, NameType } from 'recharts/types/component/DefaultTooltipContent';
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type TransactionHistogramProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}

export default function TransactionHistogram({transactions, loading, error}: TransactionHistogramProps){
    const { theme } = useContext(ThemeContext)!;
    const isDark = theme === 'dark';
    const range = 100000;

    const textColor = isDark ? '#ECF6DA' : '#273338';
    const gridColor = isDark ? '#E5E7EB' : '#344148';
    const barColor = isDark ? 'oklch(79.2% 0.209 151.711)' : 'green'
    const activeBarColor = isDark ? 'rgb(52 211 153)' : 'oklch(0.677 0.18 151.362)'

    if (error) {
        return (
            <div className="text-center py-8 text-red-500 font-mono">
                Ошибка загрузки транзакций: {error}
            </div>
        )
    }

    if (loading) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Загрузка транзакций...
            </div>
        )
    }

    if (!loading && !error && transactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Транзакций не найдено
            </div>
        )
    }

    const histogramData: Record<number, number> = {};
    transactions.map((tx) => {
        const amountRange = Math.trunc(tx.amount / range);
        histogramData[amountRange] = (histogramData[amountRange] || 0) + 1;
    })

    const total = Object.values(histogramData).reduce((a, b) => a + b, 0);
    const txData = Object.entries(histogramData)
        .map(([amount, count]) => ({
            amount: Number(amount),
            value: count,
            percent: ((count / total) * 100).toFixed(1)
        }))
        .sort((a, b) => a.amount - b.amount);

    const HistogramTooltip = ({ active, payload }: TooltipProps<ValueType, NameType>) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload;
            return (
                <div className="p-3 rounded-lg shadow-lg border dark:bg-zinc-800 dark:border-zinc-700 dark:text-sage-50 bg-white border-zinc-200 text-zinc-900">
                    <p className="text-sm opacity-80">
                        Количество: <span className="font-mono">{data.value}</span>
                    </p>
                    <p className="text-sm opacity-80">
                        Доля: <span className="font-mono">{data.percent}%</span>
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <div className="w-full h-full">
            <ResponsiveContainer width="100%" height="100%">
                <BarChart
                    data={txData}
                    margin={{ top: 30, right: 30, left: 10, bottom: 20 }}
                >
                    <CartesianGrid strokeDasharray="3 3" stroke={gridColor}/>
                    <XAxis
                        dataKey="amount"
                        tickFormatter={(val) => {
                            const min = (val * range) / 100;
                            const max = ((val + 1) * range) / 100;
                            return `${(min / 1000).toLocaleString()}k – ${(max / 1000).toLocaleString()}k`
                        }}
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                        label={{value: 'Диапазон суммы (в тысяч руб.)', position:"insideBottom", offset:-10 }}
                    />
                    <YAxis
                        dataKey="value"
                        allowDecimals={false}
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                        label={{value: 'Частота', position:"top", offset:15}}
                    />
                    <Bar
                        dataKey="value"
                        fill={barColor}
                        activeBar={{
                            fill: activeBarColor,
                            stroke: 'white',
                            strokeWidth: 2
                        }}
                        radius={[10, 10, 0, 0]}
                    />

                    <Tooltip cursor={{ fill: 'rgb(238 238 238 / 0.3)' }} content={<HistogramTooltip />} />

                </BarChart>
            </ResponsiveContainer>
        </div>
    )
}
