export type TransactionStatus = 'APPROVED' | 'DECLINED'

export type FilterStatus = 'APPROVED' | 'DECLINED'

export type TerminalType = 'POS' | 'ATM' | 'ECOM'

export type Transaction = {
    id: string
    mti: string
    stan: string
    rrn?: string
    pan: string
    processingCode: string
    processingTimeMs: number
    amount: number
    currencyCode: string
    terminalId: string
    terminalType?: TerminalType
    responseCode: string
    merchantId: string
    mcc: string
    acquirerId: string
    acquiringFee?: number
    issuerId?: string
    status: TransactionStatus
    declineReason?: string
    authCode?: string
    transmissionDateTime: string
    createdAt: string
}

export type DashboardStats = {
    totalTransactions: number
    approvedCount: number
    declinedCount: number
    approvalRate: number
    totalAmount: number
    averageAmount: number
    avgProcessingTimeMs: number
    transactionsPerMinute: number
}

export type Filter = {
    status?: FilterStatus
    dateFrom?: string
    dateTo?: string
    issuerId?: string
    mcc?: string
}

export type SearchResponse = {
    total: number
    transactions: Transaction[]
}

export type PaginationMeta = {
    currentPage: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
};
