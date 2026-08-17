import { Pie, PieChart, ResponsiveContainer, Tooltip, Cell, Legend, TooltipProps } from 'recharts';
import { ValueType, NameType } from 'recharts/types/component/DefaultTooltipContent';
import { Transaction } from '../types/index.ts'

type DeclineReasonChartProps = {
    transactions: Transaction[],
    error: string | null,
    loading: boolean,
}

export default function DeclineReasonChart({transactions, error, loading}: DeclineReasonChartProps){
    const colors = ['red', 'orange', 'yellow', 'blue', 'violet', 'pink'];

    const declineReasons: Record<string, number> = {};

    transactions.filter(tx => tx.status === 'DECLINED' && tx.declineReason)
        .map((tx) => {
            const reason = tx.declineReason as string;
            declineReasons[reason] = (declineReasons[reason] || 0) + 1;
    });

    const totalDeclined = Object.values(declineReasons).reduce((a, b) => a + b, 0);
    const reasonsData = Object.entries(declineReasons)
        .map(([reason, count]) => ({
            name: reason,
            value: count,
            percent: ((count / totalDeclined) * 100).toFixed(1)
        }))
        .sort((a, b) => b.value - a.value);

    if (error) {
        return (
            <div className="text-center py-8 text-red-500 font-mono">
                Ошибка загрузки данных об отказах: {error}
            </div>
        )
    }

    if (loading) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Загрузка данных об отказах...
            </div>
        )
    }

    if (!loading && !error && reasonsData.length === 0) {
        return (
            <div className="text-center py-8 text-gray-500 dark:text-sage-50 font-mono">
                Нет данных об отказах
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
                    data={reasonsData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    innerRadius={70}
                    outerRadius={100}
                    paddingAngle={4}
                    cornerRadius={4}
                    isAnimationActive={true}
                    activeShape={{
                        fillOpacity: 0.8,
                        stroke: 'white',
                        strokeWidth: 2
                    }}
                >
                    {reasonsData.map((_, index) => (
                        <Cell fill={colors[index % colors.length]} />
                    ))}
                </Pie>

                <Tooltip content={<PieTooltip />} />

                <Legend
                    verticalAlign="bottom"
                    height={36}
                    iconType="circle"
                    formatter={(value) => (
                        <span className="text-xs text-zinc-700 dark:text-sage-50">
                            {value}
                        </span>
                    )}
                />
            </PieChart>
        </ResponsiveContainer>
    )
}
