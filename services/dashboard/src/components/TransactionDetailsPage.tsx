import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { Transaction } from '../types';
import { AlertCircle, Receipt, CreditCard, Store, Users, Clock } from 'lucide-react';
import { getStatusIcon } from '../utils/statusIcon';
import { convertPenniesToRubles, formatDate, formatTime, hidePan } from '../utils/format';

export default function TransactionDetailsPage() {
    const { id } = useParams<{ id: string }>();

    const [tx] = useState<Transaction | null>(() => {
        if (!id) return null;

        const saved = localStorage.getItem(`tx_${id}`);
        if (saved) {
            try {
                return JSON.parse(saved);
            } catch {
                return null;
            }
        }
        return null;
    });

    if (!tx) {
        return (
            <div className="bg-zinc-200 dark:bg-sage-500 min-h-screen flex flex-col items-center justify-center">
                <div className="bg-white dark:bg-sage-400 rounded-xl p-8 shadow-2xl max-w-md text-center">
                    <AlertCircle className="mx-auto text-red-500 mb-4" size={48} />
                    <h3 className="font-bold text-xl text-gray-700 dark:text-sage-100 mb-3 font-mono">
                        Транзакция недоступна
                    </h3>
                    <p className="text-gray-600 dark:text-sage-50 mb-6">
                        Данные транзакции не найдены. Вернитесь в список и откройте её заново.
                    </p>
                </div>
            </div>
        );
    }

    const statusIconData = getStatusIcon(tx.status);
    const isApproved = tx.status === 'APPROVED';

    return (
        <div className="bg-zinc-200 dark:bg-sage-500 min-h-screen py-8 px-8">
            <div className="w-full max-w-7xl mx-auto">
                <div className="mb-6 text-center">
                    <h1 className="text-3xl font-bold text-gray-700 dark:text-sage-50 font-mono drop-shadow-lg">
                        Детали транзакции
                    </h1>
                    <p className="text-sm text-gray-500 dark:text-sage-100 mt-2 font-mono">
                        ID: <span className="font-semibold">{tx.id}</span>
                    </p>
                </div>

                <div className="space-y-5">
                    <Section title="Основная информация" icon={<Receipt size={20} />}>
                        <DataRow label="STAN" value={tx.stan} />
                        <DataRow label="RRN" value={tx.rrn || "—"} />
                        <DataRow label="PAN" value={hidePan(tx.pan)} />
                        <DataRow label="Сумма" value={convertPenniesToRubles(tx.amount)} highlight />
                        <DataRow
                            label="Статус"
                            value={
                                <span className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-semibold ${
                                    isApproved
                                        ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300'
                                        : 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300'
                                }`}>
                                    <statusIconData.icon className={statusIconData.color} size={16} />
                                    {tx.status}
                                </span>
                            }
                        />
                    </Section>

                    <Section title="Авторизация" icon={<CreditCard size={20} />}>
                        <DataRow label="Код авторизации" value={tx.authCode || "—"} />
                    </Section>

                    <Section title="Терминал и мерчант" icon={<Store size={20} />}>
                        <DataRow label="Терминал" value={`${tx.terminalId} (${tx.terminalType || "—"})`} />
                        <DataRow label="ID мерчанта" value={tx.merchantId} />
                        <DataRow label="MCC" value={tx.mcc} />
                    </Section>

                    <Section title="Участники" icon={<Users size={20} />}>
                        <DataRow label="ID эквайера" value={tx.acquirerId} />
                        <DataRow label="ID эмитента" value={tx.issuerId || "—"} />
                    </Section>

                    <Section title="Время" icon={<Clock size={20} />}>
                        <DataRow label="Дата" value={formatDate(tx.createdAt)} />
                        <DataRow label="Время" value={formatTime(tx.createdAt)} />
                    </Section>
                </div>
            </div>
        </div>
    );
}

function Section({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
    return (
        <div className="bg-white dark:bg-sage-400 rounded-xl shadow-lg overflow-hidden">
            <div className="flex items-center gap-2 px-6 py-3 bg-emerald-600 dark:bg-sage-200 text-white dark:text-sage-500 font-mono font-bold text-lg">
                {icon}
                <span>{title}</span>
            </div>
            <table className="w-full">
                <tbody>{children}</tbody>
            </table>
        </div>
    );
}

function DataRow({ label, value, highlight }: { label: string; value: React.ReactNode; highlight?: boolean }) {
    return (
        <tr className="border-b border-gray-100 dark:border-sage-300 last:border-0 hover:bg-emerald-50 dark:hover:bg-sage-300 transition-colors">
            <td className="px-6 py-3 text-sm font-semibold text-gray-600 dark:text-sage-100 font-mono whitespace-nowrap w-80 bg-zinc-50 dark:bg-sage-400/50">
                {label}
            </td>
            <td className={`px-6 py-3 text-sm text-gray-900 dark:text-sage-50 text-left font-mono ${
                highlight ? 'font-bold text-emerald-700 dark:text-emerald-300 text-base' : ''
            }`}>
                {value}
            </td>
        </tr>
    );
}
