package entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Session {

    public static final String SERVER_ADMIN_TOKEN = "SERVER-INTERNAL-" + UUID.randomUUID().toString();

    private static List<User> users = new ArrayList<User>();
    private static List<SessionUser> sessionUsers = new ArrayList<SessionUser>();
    private static List<Vaga> vagas = new ArrayList<Vaga>();
    private static List<Vehicle> vehicles = new ArrayList<Vehicle>();

    public static List<User> getUsers() {
        return users;
    }

    public static void setUsers(List<User> userList) {
        users.clear();
        if (userList != null) {
            users.addAll(userList);
        }
    }

    public static void insertUser(User user) {
        removeUser(user.getUsername());
        Session.users.add(user);
    }

    public static void removeUser(String username) {
        users.removeIf(user -> user.getUsername().equals(username));
    }

    public static void updateUserName(String username, String name) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                user.setName(name);
            }
        }
    }

    public static User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public static List<SessionUser> getSessionUsers() {
        return sessionUsers;
    }

    public static void setSessionUsers(List<SessionUser> sessionList) {
        sessionUsers.clear();
        if (sessionList != null) {
            sessionUsers.addAll(sessionList);
        }
    }

    public static void insertSessionUser(SessionUser sessionUser) {
        sessionUsers.removeIf(s -> s.getToken().equals(sessionUser.getToken()));
        Session.sessionUsers.add(sessionUser);
    }

    public static void removeSessionUser(String username) {
        sessionUsers.removeIf(sessionUser -> sessionUser.getUsername().equals(username));
    }

    public static List<Vaga> getVagas() {
        return vagas;
    }

    public static void setVagas(List<Vaga> vagaList) {
        vagas.clear();
        if (vagaList != null) {
            vagas.addAll(vagaList);
        }
    }

    public static List<Vehicle> getVehicles() {
        return vehicles;
    }

    public static void setVehicles(List<Vehicle> vehicleList) {
        vehicles.clear();
        if (vehicleList != null) {
            vehicles.addAll(vehicleList);
        }
    }

    public static boolean logout(String token) {
        for (SessionUser sessionUser : sessionUsers) {
            if (sessionUser.getToken().equals(token)) {
                removeSessionUser(sessionUser.getUsername());
                return true;
            }
        }
        return false;
    }

    public static boolean deleteUser(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                removeUser(username);
                removeSessionUser(username);
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
