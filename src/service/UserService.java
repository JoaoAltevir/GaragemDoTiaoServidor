package service;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.sql.*;

import entities.Session;
import entities.User;
import entities.SessionUser;
import dao.UserDAO;
import dao.VagaDAO;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.List;
import gui.HomeWindow;

public class UserService {

    private final UserDAO bancoUser = new UserDAO();
    private final VagaDAO vagaDAO = new VagaDAO();
    private HomeWindow home;

    public UserService(HomeWindow home) {
        this.home = home;
    }

    public UserService() {}

    public synchronized void ensureDefaultAdmin() {
        try {
            User existingAdmin = bancoUser.getUserByUsername("admin");
            if (existingAdmin == null) {
                User admin = new User();
                admin.setName("admin");
                admin.setUsername("admin");
                admin.setPassword("Admin123!");
                admin.setRole("admin");
                bancoUser.register(admin);
                Session.insertUser(admin);
            } else if (!"admin".equalsIgnoreCase(existingAdmin.getRole())) {
                existingAdmin.setRole("admin");
                Session.insertUser(existingAdmin);
            }
        } catch (Exception e) {
            System.out.println("Aviso ao verificar admin padrão: " + e.getMessage());
        }
    }

    public synchronized JsonObject register(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();

            boolean hasRole = dataObj.has("role") && !dataObj.get("role").isJsonNull();
            boolean hasToken = dataObj.has("token") && !dataObj.get("token").isJsonNull();

            String role = "user";

            if (hasRole && !hasToken) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!hasRole && hasToken) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (hasRole && hasToken) {
                String requestedRole = dataObj.get("role").getAsString().trim();
                String token = dataObj.get("token").getAsString().trim();

                if (!"admin".equals(requestedRole) && !"user".equals(requestedRole)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "O campo role não está no padrão esperado.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if (token.isEmpty()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                User loggedUser = getAuthenticatedUser(token);
                if (loggedUser == null) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Sessão expirada ou encerrada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if (!"admin".equalsIgnoreCase(loggedUser.getRole())) {
                    res.addProperty("statusCode", 401);
                    if ("admin".equals(requestedRole)) {
                        res.addProperty("message", "Operação não autorizada. Apenas administradores podem cadastrar administradores.");
                    } else {
                        res.addProperty("message", "Operação não autorizada");
                    }
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                role = requestedRole;
            }

            String name = dataObj.has("name") && !dataObj.get("name").isJsonNull() ? dataObj.get("name").getAsString() : null;
            String username = dataObj.has("username") && !dataObj.get("username").isJsonNull() ? dataObj.get("username").getAsString() : null;
            String password = dataObj.has("password") && !dataObj.get("password").isJsonNull() ? dataObj.get("password").getAsString() : null;

            if (username != null && !username.trim().isEmpty()) {
                User userExist = this.bancoUser.getUserByUsername(username);
                if (userExist != null) {
                    res.addProperty("statusCode", 409);
                    res.addProperty("message", "O username já está em uso.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            if (name == null || !validadorNome(name)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "O campo name não está no padrão esperado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (username == null || !validadorUsername(username)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "O campo username não está no padrão esperado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (password == null || !validadorSenha(password)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "O campo password não está no padrão esperado.");
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
            res.add("data", JsonNull.INSTANCE);

            Session.insertUser(user);

            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public synchronized JsonObject login(JsonObject req, String ipAddress) {
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
                res.addProperty("message", "Usuario ou senhas inválidos");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = UUID.randomUUID().toString();

            this.bancoUser.login(user.getUsername(), token, ipAddress);

            SessionUser sessionUser = new SessionUser();
            sessionUser.setUsername(user.getUsername());
            sessionUser.setToken(token);
            sessionUser.setIpAddress(ipAddress);
            Session.insertSessionUser(sessionUser);

            System.out.println("[Servidor] Token gerado para '" + user.getUsername() + "': " + token
                    + " (tokens ativos agora: " + Session.getSessionUsers().size() + ")");

            JsonObject data = new JsonObject();
            data.addProperty("token", token);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Login realizado com sucesso");
            res.add("data", data);

            return res;
        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public synchronized JsonObject logout(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Token de autenticação não fornecido.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Token de autenticação não fornecido.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString().trim();

            boolean removedFromMemory = Session.logout(token);
            String dbUsername = this.bancoUser.getUsernameByToken(token);
            if (!removedFromMemory && dbUsername == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            this.bancoUser.logout(token);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deslogado com sucesso");
            res.add("data", JsonNull.INSTANCE);

            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Token de autenticação não fornecido.");
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
            if (!hasNonBlankString(dataObj, "token")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString().trim();

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            String usernameRequested = loggedUser.getUsername();

            if (hasNonBlankString(dataObj, "username")) {
                String providedUsername = dataObj.get("username").getAsString().trim();
                if (isAdmin) {
                    usernameRequested = providedUsername;
                } else if (!providedUsername.equals(loggedUser.getUsername())) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            User user = this.bancoUser.getUserByUsername(usernameRequested);
            if (user == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado");
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

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public synchronized JsonObject updateUserName(JsonObject req) {
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

            String token = dataObj.get("token").getAsString().trim();
            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String username = hasNonBlankString(dataObj, "username")
                    ? dataObj.get("username").getAsString().trim()
                    : loggedUser.getUsername();

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String name = dataObj.has("name") && !dataObj.get("name").isJsonNull() ? dataObj.get("name").getAsString() : null;
            if (name == null || !validadorNome(name)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Nome do usuário fora do padrão");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User targetUser = bancoUser.getUserByUsername(username);
            if (targetUser == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            bancoUser.updateUserName(username, name);
            Session.updateUserName(username, name);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Nome do usuário atualizado com sucesso");
            res.add("data", JsonNull.INSTANCE);

            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Nome do usuário fora do padrão");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public synchronized JsonObject updateUserPassword(JsonObject req) {
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

            String token = dataObj.get("token").getAsString().trim();
            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String username = hasNonBlankString(dataObj, "username")
                    ? dataObj.get("username").getAsString().trim()
                    : loggedUser.getUsername();

            boolean isServerGuiAdmin = Session.SERVER_ADMIN_TOKEN.equals(token);
            if (!isServerGuiAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!isServerGuiAdmin || hasNonBlankString(dataObj, "oldPassword")) {
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
                    res.addProperty("message", "Usuário não encontrado");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            String newPassword = dataObj.has("newPassword") && !dataObj.get("newPassword").isJsonNull()
                    ? dataObj.get("newPassword").getAsString()
                    : null;

            if (newPassword == null || !validadorSenha(newPassword)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Senha fora do padrão");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            bancoUser.updateUserPassword(username, newPassword);

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Senha atualizada com sucesso");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Senha fora do padrão");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public synchronized JsonObject deleteUser(JsonObject req) {
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

            String token = dataObj.get("token").getAsString().trim();
            String username = dataObj.get("username").getAsString().trim();

            User loggedUser = getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !loggedUser.getUsername().equals(username)) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User targetUser = bancoUser.getUserByUsername(username);
            if (targetUser == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Usuário não encontrado");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (vagaDAO.hasParkedVehicleForUser(username)) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Não é permitido excluir usuário com veículo estacionado em uma vaga.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            bancoUser.deleteUser(username);
            Session.deleteUser(username);
            new VehicleService().loadAllVehicles();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Usuário deletado com sucesso");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro interno no servidor.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (IllegalArgumentException e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
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
        ensureDefaultAdmin();
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

    private boolean validadorSenha(String password) throws IllegalArgumentException, PatternSyntaxException {
        if (password == null) return false;

        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[#.*&%$@!()\\-_=+])[A-Za-z0-9#.*&%$@!()\\-_=+]{8,20}$";
        return password.matches(regex);
    }

    private boolean validadorNome(String name) throws IllegalArgumentException, PatternSyntaxException {
        if (name == null || name.trim().isEmpty() || name.length() > 60) return false;

        String regex = "^[A-Za-zÀ-ÿ ]{1,60}$";
        return name.matches(regex);
    }

    private boolean validadorUsername(String username) throws IllegalArgumentException, PatternSyntaxException {
        if (username == null) return false;

        String regex = "^[a-z0-9._]{3,20}$";
        return username.matches(regex);
    }
}
