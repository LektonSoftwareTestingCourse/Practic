import {KpiCards, KpiCardsStats} from "./KpiCards.tsx";
import {ThemeContext} from "../contexts/ThemeContext.ts";
import { useContext } from 'react';
import { Sun, Moon } from 'lucide-react';

type HeaderProps = {
    stats: KpiCardsStats,
    loading: boolean,
    error: string | null,
    isConnected: boolean,
};

export function Header({ stats, loading, error, isConnected }: HeaderProps) {
    const { theme, setTheme } = useContext(ThemeContext)!;

    let content;
    if (error) {
        content = (
            <div className="text-center py-4 text-red-500 text-lg">
                Ошибка загрузки статистики: {error}
            </div>
        );
    } else if (loading) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg dark:text-sage-100" >
                Загрузка статистики...
            </div>
        );
    } else if (!stats) {
        content = (
            <div className="text-center py-4 text-gray-500 text-lg dark:text-sage-50">
                Данные отсутствуют
            </div>
        );
    } else {
        content = <KpiCards stats={stats} />;
    }

    const isDark = theme === 'dark';

    return (
        <header className="relative flex flex-col items-center font-mono m-4 md:m-5 w-full">

            <div className="absolute top-0 left-0 ml-3 md:ml-8 mt-2 md:mt-3 flex items-center gap-2">
                <label className="group flex items-center gap-2 cursor-pointer select-none">
                    <div className="relative">
                        <input
                            type="checkbox"
                            role="switch"
                            checked={isDark}
                            onChange={() => setTheme(isDark ? 'light' : 'dark')}
                            className="peer appearance-none w-12 h-7 bg-zinc-400 checked:bg-sage-100 rounded-full cursor-pointer transition-colors outline-offset-4"
                        />
                        <div className="absolute top-0.5 left-0.5 w-6 h-6 bg-white rounded-full shadow-md transition-all duration-300 peer-checked:translate-x-5 peer-checked:bg-sage-400 flex items-center justify-center">
                            <Sun
                                className={`absolute w-4 h-4 text-zinc-400 transition-opacity duration-300 ${
                                    isDark ? 'opacity-0' : 'opacity-100'
                                }`}
                            />
                            <Moon
                                className={`absolute w-4 h-4 text-sage-50 transition-opacity duration-300 ${
                                    isDark ? 'opacity-100' : 'opacity-0'
                                }`}
                            />
                        </div>
                    </div>
                </label>
            </div>

            <div className="absolute top-0 right-0 mr-3 md:mr-8 mt-2 md:mt-3 flex items-center gap-2">
                <span
                    className={`w-7 h-7 rounded-full drop-shadow-lg ${
                        isConnected ? 'bg-[oklch(79.2%_0.209_151.711)] animate-pulse' : 'bg-[oklch(70.4%_0.191_22.216)] animate-pulse'
                    }`}
                    title={isConnected ? 'WebSocket подключен' : 'Соединение потеряно'}
                />
            </div>

            <h1 className="text-3xl md:text-5xl font-semibold drop-shadow-lg mb-6 dark:text-sage-50">Dashboard</h1>
            {content}

        </header>
    );
}
