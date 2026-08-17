import { FormEvent, useState, useEffect } from 'react';
import { Filter, FilterStatus } from "../types";
import { Listbox } from '@headlessui/react';
import { useSearchParams } from 'react-router-dom';

type FilterProps = {
    issuers: Record<string, string>;
    mccNames: Record<string, string>;
    onSearch: (filter: Filter) => void;
};

function parseFilterFromUrl(searchParams: URLSearchParams): Filter {
    return {
        status: (searchParams.get('status') as FilterStatus) || undefined,
        dateFrom: searchParams.get('dateFrom') || undefined,
        dateTo: searchParams.get('dateTo') || undefined,
        issuerId: searchParams.get('issuerId') || undefined,
        mcc: searchParams.get('mcc') || undefined,
    };
}

export function Filters({ issuers, mccNames, onSearch }: FilterProps) {
    const [searchParams] = useSearchParams();
    const [filter, setFilter] = useState<Filter>(() => parseFilterFromUrl(searchParams));

    useEffect(() => {
        setFilter(parseFilterFromUrl(searchParams));
    }, [searchParams]);

    function filterSubmit(e: FormEvent) {
        e.preventDefault();
        onSearch(filter);
    }

    function reset() {
        setFilter({});
        onSearch({});
    }

    const inputBaseClass = "border border-zinc-300 dark:border-sage-200 rounded-lg bg-zinc-300 dark:bg-sage-400 text-zinc-900 dark:text-sage-50 focus:outline-none focus:ring-2 focus:ring-emerald-500 dark:focus:ring-sage-50 focus:border-transparent cursor-pointer p-2 w-full text-left text-sm";
    const labelClass = "text-base font-bold mb-1 text-zinc-900 dark:text-sage-50";
    const buttonClass = "text-base md:text-lg font-semibold drop-shadow-lg bg-emerald-400 dark:bg-sage-200 rounded-lg dark:text-sage-50 px-4 h-10 transition-none hover:transition-colors hover:duration-200 hover:bg-emerald-500 dark:hover:bg-sage-300";

    return (
        <form onSubmit={filterSubmit} className="font-mono">
            <div className="flex flex-wrap items-center gap-4 mb-4">
                <span className="text-lg font-bold drop-shadow-lg dark:text-sage-50 flex-grow">
                    Фильтр по таблице
                </span>
            </div>

            <div className="flex flex-col xl:flex-row gap-4">

                <div className="flex-1">
                    <div className="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-4">
                        <div className="flex flex-col relative w-full">
                            <label htmlFor="status" className={labelClass}>Статус:</label>
                            <Listbox
                                value={filter.status ?? ''}
                                onChange={(val) => setFilter({ ...filter, status: (val as FilterStatus) || undefined })}
                            >
                                <Listbox.Button className={inputBaseClass}>
                                    {filter.status === 'APPROVED' && 'Одобрен'}
                                    {filter.status === 'DECLINED' && 'Отклонен'}
                                    {!filter.status && 'Все'}
                                </Listbox.Button>
                                <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800 p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                                    <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Все</Listbox.Option>
                                    <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="APPROVED">Одобрен</Listbox.Option>
                                    <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="DECLINED">Отклонен</Listbox.Option>
                                </Listbox.Options>
                            </Listbox>
                        </div>

                        <div className="flex flex-col relative w-full">
                            <label htmlFor="issuer" className={labelClass}>Банк эмитент:</label>
                            <Listbox
                                value={filter.issuerId ?? ''}
                                onChange={(val) => setFilter({ ...filter, issuerId: val || undefined })}
                            >
                                <Listbox.Button className={inputBaseClass}>
                                    {filter.issuerId ? (issuers[filter.issuerId] || filter.issuerId) : 'Все'}
                                </Listbox.Button>
                                <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800 p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                                    <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Все</Listbox.Option>
                                    {Object.keys(issuers).map((issuerId) => (
                                        <Listbox.Option key={issuerId} value={issuerId} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            {issuers[issuerId] || issuerId}
                                        </Listbox.Option>
                                    ))}
                                </Listbox.Options>
                            </Listbox>
                        </div>

                        <div className="flex flex-col relative w-full">
                            <label htmlFor="mcc" className={labelClass}>MCC:</label>
                            <Listbox
                                value={filter.mcc ?? ''}
                                onChange={(val) => setFilter({ ...filter, mcc: val || undefined })}
                            >
                                <Listbox.Button className={inputBaseClass}>
                                    {filter.mcc ? (mccNames[filter.mcc] || filter.mcc) : 'Все'}
                                </Listbox.Button>
                                <Listbox.Options className="absolute top-full left-0 mt-1 w-full rounded-lg bg-zinc-100 dark:bg-zinc-800 p-1 shadow-lg border border-zinc-200 dark:border-zinc-700 z-50 max-h-60 overflow-y-auto focus:outline-none">
                                    <Listbox.Option className="cursor-pointer rounded-md p-2 text-zinc-400 dark:text-zinc-500 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm" value="">Все</Listbox.Option>
                                    {Object.keys(mccNames).map((mccCode) => (
                                        <Listbox.Option key={mccCode} value={mccCode} className="cursor-pointer rounded-md p-2 text-zinc-700 dark:text-sage-100 hover:bg-emerald-300 dark:hover:bg-sage-500 text-sm">
                                            {mccNames[mccCode] || mccCode}
                                        </Listbox.Option>
                                    ))}
                                </Listbox.Options>
                            </Listbox>
                        </div>

                        <div className="flex flex-col relative w-full">
                            <label htmlFor="dateFrom" className={labelClass}>Начало даты:</label>
                            <input
                                className={inputBaseClass}
                                value={filter.dateFrom ?? ''}
                                onChange={(e) => setFilter({ ...filter, dateFrom: e.target.value || undefined })}
                                id="dateFrom"
                                type="date"
                            />
                        </div>

                        <div className="flex flex-col relative w-full">
                            <label htmlFor="dateTo" className={labelClass}>Конец даты:</label>
                            <input
                                className={inputBaseClass}
                                value={filter.dateTo ?? ''}
                                onChange={(e) => setFilter({ ...filter, dateTo: e.target.value || undefined })}
                                id="dateTo"
                                type="date"
                            />
                        </div>
                    </div>
                </div>

                <div className="flex flex-row gap-2 xl:self-end">
                    <button className={buttonClass} type="submit">
                        Найти
                    </button>
                    <button className={buttonClass} type="button" onClick={reset}>
                        Сбросить
                    </button>
                </div>

            </div>
        </form>
    );
}
