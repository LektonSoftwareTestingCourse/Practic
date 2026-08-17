import { AlertTriangle, RefreshCw } from 'lucide-react';

type ErrorFallbackProps = {
    name: string;
    error: Error | null;
    onRetry: () => void;
};

export function ErrorFallback({ name, error, onRetry }: ErrorFallbackProps) {
    return (
        <div
            className="
                w-full h-full min-h-[200px]
                flex flex-col items-center justify-center gap-3
                bg-zinc-100 dark:bg-sage-500
                rounded-xl border-2 border-dashed border-red-400 dark:border-red-300
                p-6 text-center
            "
            role="alert"
        >
            <AlertTriangle
                className="text-red-500 dark:text-red-300"
                size={40}
                strokeWidth={2}
                aria-hidden="true"
            />

            <h3 className="text-lg font-bold font-mono text-zinc-800 dark:text-sage-50">
                Не удалось загрузить: {name}
            </h3>

            <p className="text-sm text-zinc-600 dark:text-sage-100 max-w-md">
                {error?.message || 'Произошла непредвиденная ошибка при отображении виджета.'}
            </p>

            <button
                onClick={onRetry}
                className="
                    mt-2 inline-flex items-center gap-2
                    px-4 py-2 rounded-xl
                    bg-emerald-400 dark:bg-sage-200 dark:text-sage-500
                    font-semibold font-mono text-sm
                    cursor-pointer
                    hover:bg-emerald-500 dark:hover:bg-sage-100
                    transition-colors duration-200
                    focus:outline-none focus:ring-2 focus:ring-emerald-500
                "
            >
                <RefreshCw size={16} strokeWidth={2.5} aria-hidden="true" />
                Повторить
            </button>
        </div>
    );
}
