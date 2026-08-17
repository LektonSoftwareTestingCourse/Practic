import { CheckCircle, XCircle, type LucideIcon } from 'lucide-react';
import { TransactionStatus } from '../types/index';

type TransactionIconStatus = {
    icon: LucideIcon,
    color: string,
    label: string,
    size: number,
};

const ICON_STATUS: Record<TransactionStatus, TransactionIconStatus> = {
    'APPROVED': {
        icon: CheckCircle,
        color: "text-green-500",
        label: 'Одобрено',
        size: 18,
    },
    'DECLINED': {
        icon: XCircle,
        color: "text-red-500",
        label: 'Отклонено',
        size: 18,
    }
}

export const getStatusIcon = (status: TransactionStatus): TransactionIconStatus => {
    return ICON_STATUS[status];
};
