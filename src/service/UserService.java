package service;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.sql.*;

import entities.Session;
import entities.User;
import entities.SessionUser;
import dao.UserDAO;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import gui.HomeWindow;

public class UserService {

    private final UserDAO bancoUser = new UserDAO();
    private HomeWindow home;

    public UserService(HomeWindow home) {
        this.home = home;
    }

    public UserService() {}

    public JsonObject register(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();

            if (!hasNonBlankString(dataObj, "name") || !hasNonBlankString(dataObj, "password") || !hasNonBlankString(dataObj, "username")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String name = dataObj.get("name").getAsString();
            String password = dataObj.get("password").getAsString();
            String username = dataObj.get("username").getAsString();

            String role = "user";
            if (dataObj.has("role") && !dataObj.get("role").isJsonNull()) {
                String requestedRole = dataObj.get("role").getAsString().trim().toLowerCase();
                if (!requestedRole.isEmpty()) {
                    if (!"admin".equals(requestedRole) && !"user".equals(requestedRole)) {
                        res.addProperty("statusCode", 400);
                        res.addProperty("message", "Papel (role) inválido!");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                    role = requestedRole;
                }
            }

            if ("admin".equals(role)) {
                if (!hasNonBlankString(dataObj, "token")) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Não autorizado!");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                String token = dataObj.get("token").getAsString();
                User loggedUser = getAuthenticatedUser(token);
                if (loggedUser == null || !"admin".equalsIgnoreCase(loggedUser.getRole())) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Não autorizado!");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            boolean isValid = registerValidation(name, password, username);
            if (!isValid) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos inválidos!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User userExist = this.bancoUser.getUserByUsername(username);
            if (userExist != null) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Usuário já existe!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User user = new User();
            user.setName(name);
            user.setPassword(password);
            user.setUsername(username);
            user.setRole(role);

            this.bancoUser.register(user);

            res.addProperty("statusCode", 201);
            res.addProperty("message", "Usuário criado com sucesso");

            Session.insertUser(user);

            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao criar usuário: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject login(JsonObject req, String ipAddress) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "username") || !hasNonBlankString(dataObj, "password")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String username = dataObj.get("username").getAsString();
            String password = dataObj.get("password").getAsString();

            User user = this.bancoUser.getUserByUsername(username);

            if (user == null || !user.getPassword().equals(password)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Credenciais inválidas!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                String token = UUID.randomUUID().toString();

                this.bancoUser.login(user.getUsername(), token, ipAddress);

                JsonObject data = new JsonObject();
                data.addProperty("token", token);

                res.addProperty("statusCode", 200);
                res.addProperty("message", "Sucesso no Login");
                res.add("data", data);

                SessionUser sessionUser = new SessionUser();
                sessionUser.setUsername(user.getUsername());
                sessionUser.setToken(token);
                sessionUser.setIpAddress(ipAddress);
                Session.insertSessionUser(sessionUser);

                return res;
            }

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Senha ou usuário incorreto!");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao realizar login: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject logout(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();

            if (!Session.logout(token)) {
                String dbUsername = this.bancoUser.getUsernameByToken(token);
                if (dbUsername == null) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Sessão encerrada ou não autorizada!");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            this.bancoUser.logout(token);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deslogado com sucesso");

            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro ao realizar logout: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao realizar logout: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject getUser(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "username")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String usernameRequested = dataObj.get("username").getAsString();

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Token inválido ou não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(usernameRequested)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User user = this.bancoUser.getUserByUsername(usernameRequested);
            if (user == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject userData = new JsonObject();
            userData.addProperty("name", user.getName());
            userData.addProperty("username", user.getUsername());
            userData.addProperty("role", user.getRole() != null ? user.getRole().toLowerCase() : "user");

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário encontrado com sucesso");
            res.add("data", userData);

            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro ao buscar usuário: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject updateUserName(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "username") || !hasNonBlankString(dataObj, "name")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();
            String name = dataObj.get("name").getAsString();

            if (!validadorNome(name)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Nome inválido!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Token inválido ou não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User targetUser = bancoUser.getUserByUsername(username);
            if (targetUser == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            bancoUser.updateUserName(username, name);
            Session.updateUserName(username, name);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Nome do usuário atualizado com sucesso");

            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao atualizar nome: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject updateUserPassword(JsonObject req) {
        JsonObject res = new JsonObject();
        boolean valid;
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "username")
                    || !hasNonBlankString(dataObj, "newPassword")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();
            String newPassword = dataObj.get("newPassword").getAsString();

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!isAdmin || hasNonBlankString(dataObj, "oldPassword")) {
                if (!hasNonBlankString(dataObj, "oldPassword")) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                String oldPassword = dataObj.get("oldPassword").getAsString();
                isOldPassword(oldPassword, username);
            } else {
                User targetUser = bancoUser.getUserByUsername(username);
                if (targetUser == null) {
                    res.addProperty("statusCode", 404);
                    res.addProperty("message", "Usuário não encontrado!");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            valid = validadorSenha(newPassword);

            if (valid) {
                bancoUser.updateUserPassword(username, newPassword);

                res.addProperty("statusCode", 200);
                res.addProperty("message", "Senha atualizada com sucesso");

                return res;
            }

            res.addProperty("statusCode", 400);
            res.addProperty("message", "Senha inválida!");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro de inserção: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject deleteUser(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "username")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String username = dataObj.get("username").getAsString();

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Token inválido ou não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User targetUser = bancoUser.getUserByUsername(username);
            if (targetUser == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado!");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            bancoUser.deleteUser(username);
            Session.deleteUser(username);
            new VagaService().loadAllVagas();
            new VehicleService().loadAllVehicles();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deletado com sucesso");
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Erro ao deletar usuário: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    // FUNÇÕES AUXILIARES

    public User getAuthenticatedUser(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (Session.SERVER_ADMIN_TOKEN.equals(token)) {
            User serverAdmin = new User();
            serverAdmin.setUsername("__server_admin__");
            serverAdmin.setName("Servidor Admin");
            serverAdmin.setRole("admin");
            return serverAdmin;
        }
        String username = Session.findByToken(token);
        if (username == null) {
            username = bancoUser.getUsernameByToken(token);
        }
        if (username == null) {
            return null;
        }
        User user = bancoUser.getUserByUsername(username);
        if (user == null) {
            user = Session.findUserByUsername(username);
        }
        return user;
    }

    private boolean hasNonBlankString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() && !obj.get(key).getAsString().trim().isEmpty();
    }

    private void isOldPassword(String password, String username) throws IllegalArgumentException {
        try {
            String userPassword = bancoUser.isPassword(username);

            if (!password.equals(userPassword)) {
                throw new IllegalArgumentException("Senhas diferentes!");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private void getAllUsers() {
        try {
            List<User> users = bancoUser.getAllUsers();
            if (users != null) {
                Session.setUsers(users);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getAllSessionUsers() {
        try {
            List<SessionUser> users = bancoUser.getAllSessionUsers();
            if (users != null) {
                Session.setSessionUsers(users);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void getAll() {
        SessionService session = new SessionService();
        getAllUsers();
        getAllSessionUsers();

        VagaService vagaService = new VagaService();
        vagaService.loadAllVagas();

        VehicleService vehicleService = new VehicleService();
        vehicleService.loadAllVehicles();

        List<User> users = session.getAllUsers();
        if (this.home != null) {
            this.home.refreshUserTable(users);
            this.home.refreshVagaTable(session.getAllVagas());
            this.home.refreshVehicleTable(session.getAllVehicles());
        }
    }

    private void isValidTokenByUsername(String token, String usernameRequested) throws IllegalArgumentException {
        String username = Session.findByToken(token);
        if (username != null) {
            if (!username.equals(usernameRequested)) throw new IllegalArgumentException("Token não corresponde ao usuário solicitado!");
        } else {
            throw new IllegalArgumentException("Token inválido!");
        }
    }

    private boolean registerValidation(String name, String password, String username) {
        boolean isNameValid = validadorNome(name);
        boolean isUsernameValid = validadorUsername(username);
        boolean isPasswordValid = validadorSenha(password);

        // Só retorna true se todas as três validações forem verdadeiras
        return isNameValid && isUsernameValid && isPasswordValid;
    }

    private boolean validadorSenha(String password) throws IllegalArgumentException, PatternSyntaxException {
        if (password == null) throw new IllegalArgumentException("Senha não pode ser nula!");

        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[#.*&%$@!()\\-_=+])[A-Za-z0-9#.*&%$@!()\\-_=+]{8,20}$";
        return password.trim().matches(regex);
    }

    private boolean validadorNome(String name) throws IllegalArgumentException, PatternSyntaxException {
        if (name == null) throw new IllegalArgumentException("Nome não pode ser nulo!");

        String regex = "^[A-Za-zÀ-ÿ ]{1,60}$";
        return name.trim().matches(regex);
    }

    private boolean validadorUsername(String username) throws IllegalArgumentException, PatternSyntaxException {
        if (username == null) throw new IllegalArgumentException("Username não pode ser nulo!");

        String regex = "^[a-z0-9._]{3,20}$";
        return username.trim().matches(regex);
    }
}
