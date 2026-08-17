import { formatInTimeZone } from 'date-fns-tz';

export const hidePan = (pan: string): string => {
    return `${pan.slice(0, 4)}****${pan.slice(-4)}`;
};

export const convertPenniesToRubles = (pennies: number): string => {
    const rubles = pennies / 100;
    return rubles.toLocaleString('ru-RU', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).replace(',', '.') + ' ₽';
};

export const formatTime = (iso: string): string => {
    return formatInTimeZone(iso, 'UTC','HH:mm:ss');
};

export const formatDateTime = (iso: string): string => {
    return formatInTimeZone(iso, 'UTC','dd.MM.yyyy, HH:mm:ss');
};
export const formatDate = (iso: string): string => {
    return formatInTimeZone(iso, 'UTC', 'dd.MM.yyyy')
}
