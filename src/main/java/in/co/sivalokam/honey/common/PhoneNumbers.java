package in.co.sivalokam.honey.common;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** "+91 98765-43210", "098765 43210", "9876543210" -> "+919876543210" */
    public static String normalize(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        else if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        return "+91" + digits;
    }
}
