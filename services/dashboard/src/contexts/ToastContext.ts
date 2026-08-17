import { createContext, useContext } from 'react';
import { Toast, ToastType } from '../types/toast.ts'

export interface ToastContextType {
    toasts: Toast[];
    addToast: (message: string, type: ToastType, duration?: number) => void;
    removeToast: (id: string) => void;
}

export const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function useToastContext(){
    const context = useContext(ToastContext);
    if (!context){
        throw new Error('Use toast context within provider');
    }
    return context;
}
