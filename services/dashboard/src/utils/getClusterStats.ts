import {Transaction} from "../types";

export function getClusterStats(transactions: Transaction[]) {
    const approved = transactions.filter(t => t.status === 'APPROVED').length;
    const declined = transactions.length - approved;
    const totalAmount = transactions.reduce((sum, t) => sum + t.amount, 0);
    const approvalRate = transactions.length > 0 ? (approved / transactions.length) * 100 : 0;

    return { approved, declined, totalAmount, approvalRate };
}
