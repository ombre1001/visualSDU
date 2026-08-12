package cn.sduonline.infrastructure.redis;

public class RedisKeys {

    /**
     * 项目前缀，visual-sdu缩写
     */
    private static final String APP = "vsdu";

    /**
     * 分隔符
     */
    private static final String SEP = ":";

    /**
     * 通用 key 拼接器
     */
    public static String build(String ... parts) {
        StringBuilder sb = new StringBuilder(APP);
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                throw new IllegalArgumentException("Redis key的组成部分不能为空！");
            }
            sb.append(SEP).append(p);
        }
        return sb.toString();
    }

}
