package com.processing.transactionlogger.export;

import com.processing.common.dto.transactionlogger.TransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Сериализует транзакции в CSV (RFC 4180).
 * Первая строка - заголовок с именами колонок, далее по строке на транзакцию.
 * Поля, содержащие спецсимволы, экранируются кавычками.
 * {@code null}-значения превращаются в пустые поля.
 */
@Component
public class TransactionCsvWriter {
    private static final char DELIMITER = ',';
    private static final String LINE_END = "\r\n";
    private static final String HEADER = String.join(",", "id", "mti", "stan", "rrn", "pan", "processingCode",
            "amount", "currencyCode", "terminalId", "terminalType", "merchantId", "mcc", "acquirerId", "issuerId",
            "acquiringFee", "status", "declineReason", "authCode", "processingTimeMs", "transmissionDateTime", "createdAt");

    /**
     * Формирует CSV-документ из списка транзакций.
     *
     * @param transactions транзакции для экспорта (порядок сохраняется)
     * @return CSV-текст с заголовком. Для пустого списка - только строка заголовка.
     */
    public String toCsv(List<TransactionResponse> transactions) {
        StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
        for (TransactionResponse transaction : transactions) {
            appendRow(csv, transaction);
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder csv, TransactionResponse transaction) {
        appendField(csv, transaction.id(), true);
        appendField(csv, transaction.mti(), false);
        appendField(csv, transaction.stan(), false);
        appendField(csv, transaction.rrn(), false);
        appendField(csv, transaction.pan(), false);
        appendField(csv, transaction.processingCode(), false);
        appendField(csv, transaction.amount(), false);
        appendField(csv, transaction.currencyCode(), false);
        appendField(csv, transaction.terminalId(), false);
        appendField(csv, transaction.terminalType(), false);
        appendField(csv, transaction.merchantId(), false);
        appendField(csv, transaction.mcc(), false);
        appendField(csv, transaction.acquirerId(), false);
        appendField(csv, transaction.issuerId(), false);
        appendField(csv, transaction.acquiringFee(), false);
        appendField(csv, transaction.status(), false);
        appendField(csv, transaction.declineReason(), false);
        appendField(csv, transaction.authCode(), false);
        appendField(csv, transaction.processingTimeMs(), false);
        appendField(csv, transaction.transmissionDateTime(), false);
        appendField(csv, transaction.createdAt(), false);
        csv.append(LINE_END);
    }

    private void appendField(StringBuilder csv, Object value, boolean first) {
        if (!first) {
            csv.append(DELIMITER);
        }
        csv.append(escapeSpecialCharacters(value));
    }

    private String escapeSpecialCharacters(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.indexOf(DELIMITER) < 0 && text.indexOf('"') < 0
            && text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
