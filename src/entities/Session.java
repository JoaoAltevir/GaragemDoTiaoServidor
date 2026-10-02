package entities;

import java.util.ArrayList;
import java.util.List;

public class Session {

    private static List<User> users = new ArrayList<User>();
    private static List<SessionUser> sessionUsers = new ArrayList<SessionUser>();

    public static List<User> getUsers() {
        return users;
    }

    public static void insertUser(User user) {
        Session.users.add(user);
    }

    public static List<SessionUser> getSessionUsers() {
        return sessionUsers;
    }

    public static void insertSessionUser(SessionUser sessionUser) {
        Session.sessionUsers.add(sessionUser);
    }

    public static void removeSessionUser(String username) {
        sessionUsers.removeIf(sessionUser -> sessionUser.getUsername().equals(username));
    }

    public static boolean logout(String token){
        for (SessionUser sessionUser : sessionUsers) {
            if (sessionUser.getToken().equals(token)) {
                removeSessionUser(sessionUser.getUsername());
                return true;
            }
        }
        return false;
    }

    public static String findByToken(String token) {
        for (SessionUser sessionUser : sessionUsers) {
            if (sessionUser.getToken().equals(token)) {
                return sessionUser.getUsername();
            }
        }
        return null;
    }

}
