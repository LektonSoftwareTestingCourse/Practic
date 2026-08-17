import { useState, useEffect, useRef } from 'react';
import { Transaction } from '../types';
import { useToastContext } from '../contexts/ToastContext.ts';

type UseWebSocketOptions = {
    url?: string;
    maxRetries?: number;
    retryDelayMs?: number;
    maxRetryDelayMs?: number;
}

function calculateBackoffDelay(retryCount: number, retryDelayMs: number, maxRetryDelayMs: number): number {
    const delay = retryDelayMs * Math.pow(2, retryCount - 1);
    return Math.min(delay, maxRetryDelayMs);
}

const getDefaultWsUrl = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}/ws/transactions`;
};

export function useWebSocket({
      url = getDefaultWsUrl(),
      maxRetries = 5,
      retryDelayMs = 2000,
      maxRetryDelayMs = 30000
  }: UseWebSocketOptions = {}){
    const [liveTransactions, setLiveTransactions] = useState<Transaction[]>([]);
    const [isConnected, setIsConnected] = useState(false);

    const wsRef = useRef<WebSocket | null>(null);
    const reconnectTimeoutRef = useRef<number | null>(null);
    const retryCountRef = useRef(0);
    const isMountedRef = useRef(true);

    const { addToast } = useToastContext();

    useEffect(() => {
        isMountedRef.current = true;
        retryCountRef.current = 0;

        const connect = () => {
            const ws = new WebSocket(url);
            wsRef.current = ws;

            ws.onopen = () => {
                if (!isMountedRef.current) return;
                console.log('WebSocket подключен к transaction-logger');

                if (retryCountRef.current > 0){
                    addToast('Соединение восстановлено', 'SUCCESS');
                }
                setIsConnected(true);
                retryCountRef.current = 0;

                if (reconnectTimeoutRef.current) {
                    clearTimeout(reconnectTimeoutRef.current);
                    reconnectTimeoutRef.current = null;
                }
            };

            ws.onmessage = (event) => {
                if (!isMountedRef.current) return;
                try {
                    const newTransaction: Transaction = JSON.parse(event.data);
                    console.log('Новая транзакция:', newTransaction);

                    setLiveTransactions((prev) => [newTransaction, ...prev]);
                } catch (error) {
                    console.error('Ошибка парсинга WebSocket-сообщения:', error);
                }
            };

            ws.onerror = (error) => {
                console.error('Ошибка WebSocket:', error);
            };

            ws.onclose = (event) => {
                if (!isMountedRef.current) return;
                console.log(`WebSocket закрыт. Код: ${event.code}`);
                setIsConnected(false);
                wsRef.current = null;

                if (event.code !== 1000 && retryCountRef.current < maxRetries) {
                    retryCountRef.current++;
                    console.log(`Попытка переподключения ${retryCountRef.current}/${maxRetries}...`);

                    if (retryCountRef.current === 1){
                        addToast('Соединение потеряно. Попытка переподключения...', 'WARNING', 8000);
                    }

                    const delay = calculateBackoffDelay(retryCountRef.current, retryDelayMs, maxRetryDelayMs);
                    reconnectTimeoutRef.current = setTimeout(connect, delay);
                } else if (retryCountRef.current >= maxRetries) {
                    console.warn('Превышен лимит попыток переподключения');
                    addToast('Не удалось восстановить соединение с сервером', 'ERROR');
                }
            };
        };

        connect();

        return () => {
            console.log('Закрываем WebSocket и отменяем таймеры');
            isMountedRef.current = false;

            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            if (
                wsRef.current &&
                wsRef.current.readyState !== WebSocket.CLOSING &&
                wsRef.current.readyState !== WebSocket.CLOSED
            ) {
                wsRef.current.close(1000, 'Component unmounted');
            }
        };
    }, [url, maxRetries, retryDelayMs, maxRetryDelayMs, addToast]);

    return { liveTransactions, isConnected };
}
