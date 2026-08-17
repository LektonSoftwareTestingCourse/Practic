import { Transaction } from '../types/index.ts'
import { BarChart, XAxis, YAxis, Tooltip, CartesianGrid, Bar, ResponsiveContainer, TooltipProps } from 'recharts'
import { ValueType, NameType } from 'recharts/types/component/DefaultTooltipContent';
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type ResponseTimeHistogramProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}

export default function ResponseTimeHistogram({transactions, loading, error}: ResponseTimeHistogramProps){
    const { theme } = useContext(ThemeContext)!;
    const isDark = theme === 'dark';
    const range = 50;

    const textColor = isDark ? '#ECF6DA' : '#273338';
    const gridColor = isDark ? '#E5E7EB' : '#344148';

    if (error) {
        return (
            <div className="text-center py-8 text-red-500 font-mono">
                Ошибка загрузки данных: {error}
            </div>
        )
    }

    if (loading) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Загрузка данных...
            </div>
        )
    }

    if (!loading && !error && transactions.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Данных не найдено
            </div>
        )
    }

    const histogramData: Record<number, number> = {};
    transactions.map((tx) => {
        let procTime: number;
        if (tx.processingTimeMs >= 1000){
            procTime = 20;
        }
        else {
            procTime = Math.trunc(tx.processingTimeMs / range);
        }
        histogramData[procTime] = (histogramData[procTime] || 0) + 1;
    })

    const total = Object.values(histogramData).reduce((a, b) => a + b, 0);
    const txData = Object.entries(histogramData)
        .map(([time, count]) => ({
            name: Number(time),
            value: count,
            percent: ((count / total) * 100).toFixed(1)
        }))
        .sort((a, b) => a.name - b.name);

    const HistogramTooltip = ({ active, payload }: TooltipProps<ValueType, NameType>) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload;
            let rangeLabel;
            if (data.name === 20) {
                rangeLabel = `1000ms+`;
            } else {
                const data = payload[0].payload;
                const min = (data.name * range);
                const max = ((data.name + 1) * range);
                rangeLabel = `${(min).toLocaleString()} – ${(max).toLocaleString()}ms`
            }
            return (
                <div className="p-3 rounded-lg shadow-lg border dark:bg-zinc-800 dark:border-zinc-700 dark:text-sage-50 bg-white border-zinc-200 text-zinc-900">
                    <p className="font-bold mb-1">{rangeLabel}</p>
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
                        dataKey="name"
                        tickFormatter={(val) => {
                            if (val === 20) {
                                return `1000ms+`
                            }
                            const min = (val * range);
                            const max = ((val + 1) * range);
                            return `${(min).toLocaleString()} – ${(max).toLocaleString()}ms`
                        }}
                        stroke={textColor}
                        tick={{ fill: textColor, fontSize: 12 }}
                        tickLine={false}
                        label={{value: 'Время ответа (в ms)', position:"insideBottom", offset:-10 }}
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
                        fill='orange'
                        activeBar={{
                            fill: 'yellow',
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
