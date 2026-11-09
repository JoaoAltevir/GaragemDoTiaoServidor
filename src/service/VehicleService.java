package service;

import java.sql.SQLException;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dao.UserDAO;
import dao.VehicleDAO;
import entities.Session;
import entities.User;
import entities.Vehicle;

public class VehicleService {

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UserService userService = new UserService();

    public JsonObject createVehicle(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "brand")
                    || !hasNonBlankString(dataObj, "model") || !hasNonBlankString(dataObj, "type")
                    || !hasNonBlankString(dataObj, "licensePlate")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String brand = dataObj.get("brand").getAsString().trim();
            String model = dataObj.get("model").getAsString().trim();
            String type = dataObj.get("type").getAsString().trim().toLowerCase();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            if (!isValidType(type)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String targetUsername = loggedUser.getUsername();
            if ("admin".equalsIgnoreCase(loggedUser.getRole()) && hasNonBlankString(dataObj, "username")) {
                targetUsername = dataObj.get("username").getAsString().trim();
                User ownerUser = userDAO.getUserByUsername(targetUsername);
                if (ownerUser == null) {
                    res.addProperty("statusCode", 404);
                    res.addProperty("message", "Usuário proprietário não encontrado.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            Vehicle existing = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (existing != null) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Veículo já cadastrado com esta placa.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = new Vehicle();
            vehicle.setBrand(brand);
            vehicle.setModel(model);
            vehicle.setType(type);
            vehicle.setLicensePlate(licensePlate);
            vehicle.setUsername(targetUsername);

            vehicleDAO.createVehicle(vehicle);
            loadAllVehicles();

            res.addProperty("statusCode", 201);
            res.addProperty("message", "Veículo criado com sucesso");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject getVehicles(JsonObject req) {
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
            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            List<Vehicle> vehicles = vehicleDAO.getVehiclesByUsername(loggedUser.getUsername());
            JsonArray dataArray = new JsonArray();

            for (Vehicle v : vehicles) {
                JsonObject obj = new JsonObject();
                obj.addProperty("brand", v.getBrand());
                obj.addProperty("model", v.getModel());
                obj.addProperty("type", v.getType());
                obj.addProperty("licensePlate", v.getLicensePlate());
                dataArray.add(obj);
            }

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículos obtidos com sucesso");
            res.add("data", dataArray);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject getVehicle(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "licensePlate")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject vehicleData = new JsonObject();
            vehicleData.addProperty("brand", vehicle.getBrand());
            vehicleData.addProperty("model", vehicle.getModel());
            vehicleData.addProperty("type", vehicle.getType());
            vehicleData.addProperty("licensePlate", vehicle.getLicensePlate());

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo encontrado com sucesso");
            res.add("data", vehicleData);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject updateVehicle(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "licensePlate")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (dataObj.has("brand") && !dataObj.get("brand").isJsonNull()) {
                String brand = dataObj.get("brand").getAsString().trim();
                if (brand.isEmpty()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vehicle.setBrand(brand);
            }

            if (dataObj.has("model") && !dataObj.get("model").isJsonNull()) {
                String model = dataObj.get("model").getAsString().trim();
                if (model.isEmpty()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vehicle.setModel(model);
            }

            if (dataObj.has("type") && !dataObj.get("type").isJsonNull()) {
                String type = dataObj.get("type").getAsString().trim().toLowerCase();
                if (!isValidType(type)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vehicle.setType(type);
            }

            vehicleDAO.updateVehicle(vehicle);
            loadAllVehicles();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo atualizado com sucesso");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public JsonObject deleteVehicle(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "licensePlate")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            if (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Não autorizado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            vehicleDAO.deleteVehicle(licensePlate);
            loadAllVehicles();
            new VagaService().loadAllVagas();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo removido com sucesso");
            res.add("data", JsonNull.INSTANCE);
            return res;

        } catch (SQLException e) {
            res.addProperty("statusCode", 500);
            res.addProperty("message", "Erro de banco: " + e.getMessage());
            res.add("data", JsonNull.INSTANCE);
            return res;
        } catch (Exception e) {
            res.addProperty("statusCode", 400);
            res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
            res.add("data", JsonNull.INSTANCE);
            return res;
        }
    }

    public void loadAllVehicles() {
        try {
            List<Vehicle> vehicles = vehicleDAO.getAllVehicles();
            if (vehicles != null) {
                Session.setVehicles(vehicles);
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar veículos: " + e.getMessage());
        }
    }

    private boolean hasNonBlankString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() && !obj.get(key).getAsString().trim().isEmpty();
    }

    private boolean isValidType(String type) {
        return "eletrico".equals(type) || "combustao".equals(type);
    }
}
