export const exportToCsv = (filename: string, rows: Record<string, unknown>[]): void => {
    if (!rows || !rows.length) {
        console.warn('Нет данных для экспорта');
        return;
    }

    const separator = ';';

    const keys = Object.keys(rows[0]);

    const csvContent =
        'sep=;\n' +
        keys.join(separator) + '\n' +
        rows.map(row => {
            return keys.map(k => {
                const value = row[k];

                let cell = value === null || value === undefined
                    ? ''
                    : String(value).replace(/"/g, '""');

                if (cell !== '') {
                    cell = `="${cell}"`;
                }

                return cell;
            }).join(separator);
        }).join('\n');

    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
};
