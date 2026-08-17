import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { useEffect, useContext } from 'react';
import { CityCluster, useLocations } from '../hooks/useLocations';
import { Transaction } from '../types';
import { getStatusIcon } from '../utils/statusIcon';
import 'leaflet/dist/leaflet.css';
import { convertPenniesToRubles } from "../utils/format.ts";
import { ThemeContext } from "../contexts/ThemeContext.ts";
import {getClusterStats} from "../utils/getClusterStats.ts";

const LIGHT_TILES = {
    url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
};

const DARK_TILES = {
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
};

type TransactionMapProps = {
    transactions: Transaction[];
};

function getClusterStyle(count: number) {
    if (count >= 10) {
        return {
            color: '#1e40af',
            shadow: 'rgba(30, 64, 175, 0.6)',
            size: 40,
            fontSize: '14px',
        };
    } else if (count >= 5) {
        return {
            color: '#3b82f6',
            shadow: 'rgba(59, 130, 246, 0.5)',
            size: 35,
            fontSize: '13px',
        };
    } else {
        return {
            color: '#60a5fa',
            shadow: 'rgba(96, 165, 250, 0.4)',
            size: 30,
            fontSize: '12px',
        };
    }
}

function createClusterIcon(count: number) {
    const style = getClusterStyle(count);

    return L.divIcon({
        className: 'custom-cluster-marker',
        html: `
            <div style="
                position: relative;
                width: ${style.size}px;
                height: ${style.size}px;
                background: ${style.color};
                border: 4px solid white;
                border-radius: 50%;
                box-shadow: 0 0 0 3px ${style.color}, 0 4px 12px ${style.shadow};
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                font-weight: bold;
                font-size: ${style.fontSize};
                font-family: system-ui, -apple-system, sans-serif;
            ">
                ${count}
            </div>
        `,
        iconSize: [style.size, style.size],
        iconAnchor: [style.size / 2, style.size / 2],
        popupAnchor: [0, -style.size / 2],
    });
}

function FitBoundsToClusters({ clusters }: { clusters: CityCluster[] }) {
    const map = useMap();

    useEffect(() => {
        if (clusters.length === 0) return;

        const bounds = L.latLngBounds(clusters.map(c => c.coordinates));
        map.fitBounds(bounds, {
            padding: [80, 80],
            maxZoom: 10,
        });
    }, [clusters, map]);

    return null;
}

export function TransactionMap({ transactions }: TransactionMapProps) {
    const clusters = useLocations(transactions);
    const { theme } = useContext(ThemeContext)!;

    const isDark = theme === 'dark';
    const tiles = isDark ? DARK_TILES : LIGHT_TILES;

    const initialCenter: [number, number] = [61.5240, 105.3188];
    const initialZoom = 3;

    if (clusters.length === 0) {
        return (
            <div className="w-full h-full min-h-[400px] rounded-lg overflow-hidden flex items-center justify-center bg-zinc-100 dark:bg-sage-500">
                <div className="text-center text-gray-500 dark:text-sage-50">
                    <div className="text-lg font-mono">Ожидание транзакций...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="w-full h-full min-h-[400px] rounded-lg overflow-hidden">
            <MapContainer
                center={initialCenter}
                zoom={initialZoom}
                style={{ height: '100%', width: '100%' }}
                scrollWheelZoom={true}
            >
                <TileLayer
                    key={theme}
                    attribution={tiles.attribution}
                    url={tiles.url}
                />

                <FitBoundsToClusters clusters={clusters} />

                {clusters.map((cluster) => {
                    const icon = createClusterIcon(cluster.count);
                    const stats = getClusterStats(cluster.transactions);

                    return (
                        <Marker
                            key={`${cluster.city}-${theme}`}
                            position={cluster.coordinates}
                            icon={icon}
                        >
                            <Popup className={isDark ? 'dark-popup' : ''}>
                                <div className="min-w-[280px] text-[13px] text-zinc-900 dark:text-zinc-200">
                                    <div className="font-bold text-[16px] mb-2.5 pb-1.5 border-b-2 border-zinc-200 dark:border-zinc-700 flex items-center justify-between">
                                        <span>{cluster.city}</span>
                                        <span
                                            className="text-white px-2 py-0.5 rounded-xl text-[12px]"
                                            style={{ background: getClusterStyle(cluster.count).color }}
                                        >
                                            {cluster.count} транз.
                                        </span>
                                    </div>

                                    <div className="grid grid-cols-2 gap-2 mb-2.5 p-2 bg-zinc-50 dark:bg-zinc-800 rounded-md">
                                        <div>
                                            <div className="text-[11px] text-zinc-500 dark:text-zinc-400">Одобрено</div>
                                            <div className="font-bold text-green-600">{stats.approved}</div>
                                        </div>
                                        <div>
                                            <div className="text-[11px] text-zinc-500 dark:text-zinc-400">Отклонено</div>
                                            <div className="font-bold text-red-600">{stats.declined}</div>
                                        </div>
                                        <div>
                                            <div className="text-[11px] text-zinc-500 dark:text-zinc-400">Общая сумма</div>
                                            <div className="font-bold">
                                                {convertPenniesToRubles(stats.totalAmount)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="text-[11px] text-zinc-500 dark:text-zinc-400">Одобрение</div>
                                            <div className="font-bold">{stats.approvalRate.toFixed(1)}%</div>
                                        </div>
                                    </div>

                                    <div className="max-h-[200px] overflow-y-auto border-t border-zinc-200 dark:border-zinc-700 pt-2">
                                        <div className="text-[11px] text-zinc-500 dark:text-zinc-400 mb-1.5">
                                            Транзакции:
                                        </div>
                                        {cluster.transactions.map((tx) => {
                                            const statusIconData = getStatusIcon(tx.status);
                                            const StatusIcon = statusIconData.icon;

                                            return (
                                                <div
                                                    key={tx.id}
                                                    className="py-1 border-b border-zinc-100 dark:border-zinc-700 text-[12px]"
                                                >
                                                    <div className="flex justify-between items-center">
                                                        <span className="text-zinc-500 dark:text-zinc-400">
                                                            {tx.terminalId}
                                                        </span>
                                                        <StatusIcon
                                                            className={statusIconData.color}
                                                            size={16}
                                                            aria-hidden="true"
                                                        />
                                                    </div>
                                                    <div className="text-zinc-700 dark:text-zinc-300">
                                                        {convertPenniesToRubles(tx.amount)}
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            </Popup>
                        </Marker>
                    );
                })}
            </MapContainer>
        </div>
    );
}
