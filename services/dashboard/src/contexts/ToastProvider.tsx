import { useState, useCallback, ReactNode, useRef } from 'react'
import { Toast, ToastType } from '../types/toast.ts'
import { ToastContext } from './ToastContext.ts'

interface ToastProviderProps {
    children: ReactNode;
}

export function ToastProvider ({children}: ToastProviderProps) {
    const [toasts, setToasts] = useState<Toast[]>([]);
    const timers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

    const clearTimer = useCallback((id: string) => {
        const currentTimer = timers.current[id];
        if(currentTimer){
            clearTimeout(currentTimer);
            delete timers.current[id]
        }
    }, []);

    const removeToast = useCallback((id: string) => {
        clearTimer(id);
        setToasts(prev => prev.filter(t => t.id !== id));
    }, [clearTimer]);

    const addToast = useCallback((message: string, type: ToastType, duration?: number) => {
        const durationToast = duration ?? 4000;
        const toastId = () => {
            if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
                return crypto.randomUUID();
            }
            return `${Math.random().toString(36).substring(2, 9) + Date.now().toString(36)}`;
        };
        const newToast: Toast = {id: toastId(), message, type, duration: durationToast};
        setToasts(prev => [...prev, newToast]);

        timers.current[newToast.id] = setTimeout(() => {
            removeToast(newToast.id);
        }, newToast.duration);
    }, [removeToast]);

    return (
        <ToastContext.Provider value={{toasts, addToast, removeToast}}>
            {children}
        </ToastContext.Provider>
    );
}
