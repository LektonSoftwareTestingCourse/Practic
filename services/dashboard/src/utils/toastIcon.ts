import { TriangleAlert, SquareX, MessageCircleCheck, Info, type LucideIcon } from 'lucide-react';
import { ToastType } from '../types/toast.ts';

type ToastIconType = {
    icon: LucideIcon,
    backgroundColor: string,
    borderColor: string;
    titleColor: string;
    messageColor: string;
    iconColor: string;
    label: string;
}

const ICON_TYPE: Record<ToastType, ToastIconType> = {
    'SUCCESS': {
        icon: MessageCircleCheck,
        backgroundColor: 'bg-green-50',
        borderColor: 'border-green-500',
        titleColor: 'text-green-800',
        messageColor: 'text-green-700',
        iconColor: '#006303',
        label: 'SUCCESS',
    },
    'ERROR': {
        icon: SquareX,
        backgroundColor: 'bg-red-50',
        borderColor: 'border-red-500',
        titleColor: 'text-red-800',
        messageColor: 'text-red-700',
        iconColor: '#680000',
        label: 'ERROR',
    },
    'WARNING': {
        icon: TriangleAlert,
        backgroundColor: 'bg-orange-50',
        borderColor: 'border-orange-500',
        titleColor: 'text-orange-800',
        messageColor: 'text-orange-700',
        iconColor: '#ae4e00',
        label: 'WARNING',
    },
    'INFO': {
        icon: Info,
        backgroundColor: 'bg-blue-50',
        borderColor: 'border-blue-500',
        titleColor: 'text-blue-800',
        messageColor: 'text-blue-700',
        iconColor: '#0071ae',
        label: 'INFO',
    }
}

export const getTypeIcon = (type: ToastType): ToastIconType => {
    return ICON_TYPE[type];
}
