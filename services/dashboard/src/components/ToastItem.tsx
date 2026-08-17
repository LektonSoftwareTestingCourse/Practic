import { Toast } from '../types/toast.ts';
import { getTypeIcon } from '../utils/toastIcon.ts';
import { X } from 'lucide-react';

type ToastItemProps = {
    toast: Toast,
    onClose: (id: string) => void;
}

export function ToastItem({toast, onClose} : ToastItemProps) {
    const toastIcon = getTypeIcon(toast.type);
    const Icon = toastIcon.icon;

    return (
        <div className={`flex items-start gap-3 p-4 rounded-r-lg rounded-l-sm
                        ${toastIcon.backgroundColor} border-l-4 ${toastIcon.borderColor} shadow-lg
                        max-w-md w-full`} >
            <div className="flex-shrink-0 mt-0.5">
                <Icon stroke={toastIcon.iconColor} size={36} />
            </div>

            <div className="flex-1 min-w-0">
                <p className={`text-xs font-bold uppercase tracking-wider ${toastIcon.titleColor}`}>
                    {toastIcon.label}
                </p>
                <p className={`text-sm mt-1 leading-relaxed break-words ${toastIcon.messageColor}`}>
                    {toast.message}
                </p>
            </div>

            <button
                onClick={() => onClose(toast.id)}
                aria-label="Закрыть уведомление"
                className="flex-shrink-0 ml-2 p-1 rounded-full text-red-400 hover:text-red-600 hover:bg-red-100 transition-all"
            >
                <X size={16}/>
            </button>
        </div>
    )
};
