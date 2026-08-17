type FetchApiOptions = {
    timeout?: number;
    retries?: number;
    onError?: (message: string) => void;
}

type InternalFetchOptions = FetchApiOptions & {
    headers?: Record<string, string>;
}

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

async function fetchWithRetry(route: string, options: InternalFetchOptions = {}): Promise<Response> {
    const { timeout = 5000, retries = 2, headers } = options;
    let lastError: Error | null = null;

    for (let att = 0; att <= retries; att++) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeout);

        try {
            const response = await fetch(route, {
                signal: controller.signal,
                headers,
            });
            clearTimeout(timeoutId);

            if (!response.ok) {
                if (response.status >= 400 && response.status < 500) {
                    throw new Error(`Client Error ${response.status}: ${response.statusText}`);
                }
                if (response.status === 503 && att < retries) {
                    await wait(2000);
                    continue;
                }
                throw new Error(`Server Error ${response.status}: ${response.statusText}`);
            }

            return response;
        } catch (err) {
            clearTimeout(timeoutId);
            const error = err instanceof Error ? err : new Error(String(err));
            lastError = error;

            if (error.message.startsWith('Client Error')){
                if (options.onError){
                    options.onError(lastError.message);
                }
                throw error;
            }
            if (error.name === 'AbortError' || error.message.includes('Failed to fetch')) {
                if (att < retries) {
                    await wait(2000);
                    continue;
                }
            }
            if (options.onError) {
                options.onError(error.message);
            }

            throw error;
        }
    }

    throw lastError;
}

export async function fetchApi<T>(route: string, options: FetchApiOptions = {}): Promise<T> {
    const response = await fetchWithRetry(route, options);
    return await response.json() as T;
}

export async function fetchApiBlob(route: string, options: FetchApiOptions = {}): Promise<Blob> {
    const response = await fetchWithRetry(route, {
        ...options,
        headers: { Accept: 'text/csv' },
    });
    return await response.blob();
}

export default fetchApi;
