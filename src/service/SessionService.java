package service;

import entities.User;
import entities.SessionUser;
import entities.Session;

import java.util.List;

public class SessionService {

    public List<User> getAllUsers(){
        return Session.getUsers();
    }

    public void insertUsers(List<User> users){
        for( User user : users){
            Session.insertUser(user);
        }
    }

    public void insertSessionUsers(List<SessionUser> users){
        
    }

    public SessionUser isLogged(String username) {
        List<SessionUser> sessionUsers = Session.getSessionUsers();
        for (SessionUser sessionUser : sessionUsers) {
            if (sessionUser.getUsername().equals(username)) {
                return sessionUser;
            }
        }
        return null;
    }


}
