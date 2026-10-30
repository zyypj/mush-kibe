package br.com.muxe.core.bukkit.credit;

public final class CreditFormat {

    private CreditFormat() {
    }

    public static String format(long amount) {
        String digits = Long.toString(Math.abs(amount));
        StringBuilder grouped = new StringBuilder();
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && (digits.length() - index) % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(digits.charAt(index));
        }
        return amount < 0 ? "-" + grouped : grouped.toString();
    }
}
