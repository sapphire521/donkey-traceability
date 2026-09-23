package cn.donkeychain.trace.common;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();

    public static void set(Long userId, String username, String role, Long orgId) {
        USER_ID.set(userId); USERNAME.set(username); ROLE.set(role); ORG_ID.set(orgId);
    }
    public static Long userId() { return USER_ID.get(); }
    public static String username() { return USERNAME.get(); }
    public static String role() { return ROLE.get(); }
    public static Long orgId() { return ORG_ID.get(); }
    public static void clear() { USER_ID.remove(); USERNAME.remove(); ROLE.remove(); ORG_ID.remove(); }
}
