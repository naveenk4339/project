package com.pos.receipt;

import com.pos.common.events.PaymentSummary;
import com.pos.common.events.TransactionCompleted;
import com.pos.common.events.TransactionLine;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders a 40-column receipt, the width of a standard 80mm thermal printer. */
@Component
public class ReceiptRenderer {

    static final int WIDTH = 40;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

    public String render(TransactionCompleted sale, boolean refunded) {
        StringBuilder out = new StringBuilder();
        center(out, "POS DEMO STORE");
        center(out, sale.storeId() + " / " + sale.terminalId());
        center(out, TIME.format(sale.completedAt()) + " UTC");
        rule(out);
        for (TransactionLine line : sale.lines()) {
            row(out, line.quantity() + " x " + line.name(), money(line.unitPriceCents() * line.quantity()));
            if (line.discountCents() > 0) {
                row(out, "   discount", "-" + money(line.discountCents()));
            }
        }
        rule(out);
        row(out, "Subtotal", money(sale.subtotalCents()));
        if (sale.discountCents() > 0) {
            row(out, "Savings", "-" + money(sale.discountCents()));
        }
        row(out, "Tax", money(sale.taxCents()));
        row(out, "TOTAL", money(sale.totalCents()));
        PaymentSummary p = sale.payment();
        if (p != null) {
            if ("CASH".equals(p.method())) {
                row(out, "Cash", money(p.tenderedCents()));
                row(out, "Change", money(p.changeCents()));
            } else {
                row(out, p.cardBrand() + " ****" + p.cardLast4(), money(p.amountCents()));
                row(out, "Auth", p.authCode());
            }
        }
        if (!sale.appliedPromotions().isEmpty()) {
            out.append("Promotions: ").append(String.join(", ", sale.appliedPromotions())).append('\n');
        }
        rule(out);
        center(out, sale.transactionId());
        if (refunded) {
            center(out, "*** REFUNDED ***");
        }
        center(out, "Thank you!");
        return out.toString();
    }

    private static String money(long cents) {
        return String.format(Locale.US, "$%,.2f", cents / 100.0);
    }

    private static void row(StringBuilder out, String left, String right) {
        int space = WIDTH - right.length() - 1;
        String l = left.length() > space ? left.substring(0, space) : left;
        out.append(l).append(" ".repeat(WIDTH - l.length() - right.length())).append(right).append('\n');
    }

    private static void center(StringBuilder out, String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        out.append(" ".repeat(pad)).append(text).append('\n');
    }

    private static void rule(StringBuilder out) {
        out.append("-".repeat(WIDTH)).append('\n');
    }
}
