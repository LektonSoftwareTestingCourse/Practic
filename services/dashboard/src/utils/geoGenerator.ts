export const ISSUER_LOCATIONS: Record<string, { city: string; coordinates: [number, number] }> = {
    ISS001: { city: 'Санкт-Петербург', coordinates: [59.9386, 30.3141] },
    ISS002: { city: 'Ухта', coordinates: [63.5671, 53.6835] },
    ISS003: { city: 'Красноярск', coordinates: [56.0184, 92.8672] },
    ISS004: { city: 'Калининград', coordinates: [54.7065, 20.511] },
    ISS005: { city: 'Владивосток', coordinates: [43.1056, 131.874] },
};

export const DEFAULT_LOCATION: { city: string; coordinates: [number, number] } = {
    city: 'Москва',
    coordinates: [55.7558, 37.6173],
};

export function getTransactionLocation(issuerId?: string): { city: string; coordinates: [number, number] } {
    if (!issuerId) return DEFAULT_LOCATION;
    return ISSUER_LOCATIONS[issuerId] ?? DEFAULT_LOCATION;
}
