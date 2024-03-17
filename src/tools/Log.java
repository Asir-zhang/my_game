package tools;

public class Log {

    /**
     * 打印错误日志
     *
     * @param message 带有占位符{}的日志信息模板
     * @param args    替换占位符的可变参数列表
     */
    public static void error(String message, Object... args) {
        String formattedMessage = message.replace("{}", "%s");
        System.err.println(String.format(formattedMessage, args));
    }

    /**
     * 打印警示日志
     *
     * @param message 带有占位符{}的日志信息模板
     * @param args    替换占位符的可变参数列表
     */
    public static void warning(String message, Object... args) {
        String formattedMessage = message.replace("{}", "%s");
        System.out.println(String.format(formattedMessage, args));
    }
}
