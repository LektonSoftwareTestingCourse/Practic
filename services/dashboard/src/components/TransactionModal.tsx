import { Transaction } from "../types";
import { getStatusIcon } from "../utils/statusIcon.ts";
import { convertPenniesToRubles, formatDate, formatTime, hidePan } from "../utils/format.ts";
import { Fragment } from 'react';
import { ExternalLink } from 'lucide-react';

type TransactionModalProps = {
    transaction: Transaction;
    onClose: () => void;
}

export function TransactionModal({ transaction, onClose }: TransactionModalProps) {
    const statusIconData = getStatusIcon(transaction.status);

    const rows = [
        {label: "STAN", value: transaction.stan },
        {label: "RRN", value: transaction.rrn || "—" },
        {label: "PAN", value: hidePan(transaction.pan) },
        {label: "Сумма", value: convertPenniesToRubles(transaction.amount) },
        {label: "Статус", value: (
                <div className="flex items-center gap-2 justify-end">
                    <statusIconData.icon className={statusIconData.color} size={statusIconData.size} />
                    <span>{transaction.status}</span>
                </div>
            ),},
        {label: "Код авторизации", value: transaction.authCode || "—"  },
        {label: "Терминал", value: (
                <div>{transaction.terminalId} ({transaction.terminalType || "—"})</div>
            )},
        {label: "ID мерчанта", value: transaction.merchantId },
        {label: "MCC", value: transaction.mcc },
        {label: "ID экваера", value: transaction.acquirerId},
        {label: "ID эмитента", value: transaction.issuerId || "—" },
        {label: "Время", value: formatTime(transaction.createdAt) },
        {label: "Дата", value: formatDate(transaction.createdAt) }
    ];

    const handleOpenInNewTab = () => {
        const maskedTransaction = {
            ...transaction,
            pan: hidePan(transaction.pan)
        };
        localStorage.setItem(`tx_${transaction.id}`, JSON.stringify(maskedTransaction));
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center z-[2000]" onClick={onClose}>
            <div className="bg-white dark:bg-sage-500 rounded-xl p-4 md:p-6 shadow-2xl max-w-md w-full mx-4 relative"
                 onClick={(e) => e.stopPropagation()} >

                <a
                    href={`/transaction/${transaction.id}`}
                    target="_blank"
                    onClick={handleOpenInNewTab}
                    className="absolute top-4 right-4 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-sage-300 transition-colors"
                    title="Открыть в новой вкладке"
                >
                    <ExternalLink size={20} className="text-gray-600 dark:text-sage-100" />
                </a>

                <h3 className="font-bold text-xl text-gray-600 dark:text-sage-100 m-5 text-center font-mono">
                    Детали транзакции
                </h3>
                <div className="grid grid-cols-[auto_1fr] gap-x-3 md:gap-x-6 gap-y-2 md:gap-y-3">
                    {rows.map((row) => (
                        <Fragment key={row.label}>
                            <dt className="font-semibold text-gray-600 dark:text-sage-100 text-left whitespace-nowrap pl-5 font-mono">
                                {row.label}:
                            </dt>
                            <dd className="text-gray-900 dark:text-sage-50 text-right whitespace-nowrap pr-5">
                                {row.value}
                            </dd>
                        </Fragment>
                    ))}
                </div>

                <button
                    onClick={onClose}
                    className="mt-6 w-full py-2 bg-emerald-600 dark:bg-sage-300 hover:bg-emerald-700 dark:hover:bg-sage-200 text-white dark:text-sage-50 rounded-xl transition-colors"
                >
                    ✕ Закрыть
                </button>
            </div>
        </div>
    );
}
