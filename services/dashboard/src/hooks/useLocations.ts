import { useMemo } from 'react';
import { Transaction } from '../types';
import { getTransactionLocation } from '../utils/geoGenerator';

export type CityCluster = {
    city: string;
    coordinates: [number, number];
    transactions: Transaction[];
    count: number;
};

export function useLocations(transactions: Transaction[]): CityCluster[] {
    return useMemo(() => {
        const cityMap = new Map<string, CityCluster>();

        for (const tx of transactions) {
            const location = getTransactionLocation(tx.issuerId);
            const key = location.city;

            if (!cityMap.has(key)) {
                cityMap.set(key, {
                    city: location.city,
                    coordinates: location.coordinates,
                    transactions: [],
                    count: 0,
                });
            }

            const cluster = cityMap.get(key)!;
            cluster.transactions.push(tx);
            cluster.count++;
        }

        return Array.from(cityMap.values());
    }, [transactions]);
}
