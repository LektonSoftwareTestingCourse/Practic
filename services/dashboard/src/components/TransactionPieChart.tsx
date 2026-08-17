import { Pie, PieChart, Tooltip, Cell, Legend, ResponsiveContainer, TooltipProps } from 'recharts';
import { ValueType, NameType } from 'recharts/types/component/DefaultTooltipContent';
import { Transaction } from '../types/index.ts'
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';

type PieChartProps = {
    transactions: Transaction[];
    loading: boolean;
    error: string | null
}
export default function TransactionPieChart({transactions, loading, error}: PieChartProps) {
    const { theme } = useContext(ThemeContext)!;
    const isDark = theme === 'dark';
    const cellColorApproved = isDark ? 'oklch(79.2% 0.209 151.711)' : 'green'
    const cellColorDeclined = isDark ? 'oklch(70.4% 0.191 22.216)' : 'red'

    const approved = transactions?.filter((s) => s.status === 'APPROVED').length || 0;
    const declined = transactions?.filter((s) => s.status === 'DECLINED').length || 0;

    if (error) {
        return (
            <div className="text-center py-8 text-red-500">
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

    const PieTooltip = ({ active, payload }: TooltipProps<ValueType, NameType>) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload;
            return (
                <div className="p-3 rounded-lg shadow-lg border dark:bg-zinc-800 dark:border-zinc-700 dark:text-sage-50 bg-white border-zinc-200 text-zinc-900">
                    <p className="font-bold mb-1">{data.name}</p>
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
        <ResponsiveContainer width="100%" height="100%" >
        <PieChart margin={{ top: 10, right: 10, left: 10, bottom: 10 }}>
            <Pie
                data={[
                    { name: 'APPROVED', value: approved, percent: ((approved / (approved + declined)) * 100).toFixed(1) },
                    { name: 'DECLINED', value: declined, percent: ((declined / (approved + declined)) * 100).toFixed(1) },
                ]}
                dataKey="value"
                isAnimationActive={true}
                cx="50%"
                cy="45%"
                outerRadius={100}
                label={false}
                activeShape={{
                    fillOpacity: 0.8,
                    stroke: 'white',
                    strokeWidth: 2
                }}
            >
                <Cell fill={cellColorApproved}/>
                <Cell fill={cellColorDeclined} />
            </Pie>

            <Tooltip content={<PieTooltip />} />

            <Legend
                layout="horizontal"
                align="center"
                verticalAlign="bottom"
                iconType="circle"
                formatter={(value) => {
                    const count = value === 'APPROVED' ? approved : declined;
                    const label = value === 'APPROVED' ? 'Одобрено' : 'Отклонено';
                    return (
                        <span className="text-zinc-700 font-mono dark:text-sage-50">
                                { `${label}(${count})`}
                            </span>
                    )
                }
                }
            />
            <Tooltip/>
        </PieChart>
    </ResponsiveContainer>
    )
}
