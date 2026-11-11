package service;

import java.sql.SQLException;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dao.UserDAO;
import dao.VagaDAO;
import dao.VehicleDAO;
import entities.Session;
import entities.User;
import entities.Vaga;
import entities.Vehicle;

public class VehicleService {

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final VagaDAO vagaDAO = new VagaDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UserService userService = new UserService();

    public synchronized JsonObject createVehicle(JsonObject req) {
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
            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isServerGuiAdmin = Session.SERVER_ADMIN_TOKEN.equals(token);
            if (!isServerGuiAdmin) {
                if (!"user".equalsIgnoreCase(loggedUser.getRole())) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada. Apenas usuários comuns podem cadastrar veículos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                if (dataObj.has("username") || dataObj.has("ownerUsername")) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            if (!hasNonBlankString(dataObj, "brand") || !hasNonBlankString(dataObj, "type")
                    || !hasNonBlankString(dataObj, "licensePlate")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String brand = dataObj.get("brand").getAsString().trim();
            String type = dataObj.get("type").getAsString().trim();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            String model = null;
            if (dataObj.has("model") && !dataObj.get("model").isJsonNull()) {
                model = dataObj.get("model").getAsString().trim();
                if (model.length() > 20) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            if (!isValidBrand(brand) || !isValidType(type) || !isValidLicensePlate(licensePlate)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String targetUsername = loggedUser.getUsername();
            if (isServerGuiAdmin && hasNonBlankString(dataObj, "username")) {
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
            if (!hasNonBlankString(dataObj, "token") || dataObj.has("username") || dataObj.has("ownerUsername")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString().trim();
            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!"user".equalsIgnoreCase(loggedUser.getRole())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada. Apenas usuários comuns podem consultar veículos próprios.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            List<Vehicle> vehicles = vehicleDAO.getVehiclesByUsername(loggedUser.getUsername());
            JsonArray dataArray = new JsonArray();

            for (Vehicle v : vehicles) {
                JsonObject obj = new JsonObject();
                obj.addProperty("brand", v.getBrand());
                if (v.getModel() == null) {
                    obj.add("model", JsonNull.INSTANCE);
                } else {
                    obj.addProperty("model", v.getModel());
                }
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

            String token = dataObj.get("token").getAsString().trim();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());

            if (vehicle == null || (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername()))) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject vehicleData = new JsonObject();
            vehicleData.addProperty("brand", vehicle.getBrand());
            if (vehicle.getModel() == null) {
                vehicleData.add("model", JsonNull.INSTANCE);
            } else {
                vehicleData.addProperty("model", vehicle.getModel());
            }
            vehicleData.addProperty("type", vehicle.getType());
            vehicleData.addProperty("licensePlate", vehicle.getLicensePlate());

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo encontrado com sucesso");
            res.add("data", vehicleData);
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

    public synchronized JsonObject updateVehicle(JsonObject req) {
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

            String token = dataObj.get("token").getAsString().trim();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isServerGuiAdmin = Session.SERVER_ADMIN_TOKEN.equals(token);
            if (!isServerGuiAdmin) {
                if (!"user".equalsIgnoreCase(loggedUser.getRole())) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada. Apenas usuários comuns podem atualizar veículos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                if (dataObj.has("username") || dataObj.has("ownerUsername")) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null || (!isServerGuiAdmin && !vehicle.getUsername().equals(loggedUser.getUsername()))) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String newBrand = vehicle.getBrand();
            if (dataObj.has("brand")) {
                if (dataObj.get("brand").isJsonNull()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                newBrand = dataObj.get("brand").getAsString().trim();
                if (!isValidBrand(newBrand)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            }

            String newModel = vehicle.getModel();
            if (dataObj.has("model")) {
                if (dataObj.get("model").isJsonNull()) {
                    newModel = null;
                } else {
                    newModel = dataObj.get("model").getAsString().trim();
                    if (newModel.length() > 20) {
                        res.addProperty("statusCode", 400);
                        res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                }
            }

            String newType = vehicle.getType();
            if (dataObj.has("type")) {
                if (dataObj.get("type").isJsonNull()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                newType = dataObj.get("type").getAsString().trim();
                if (!isValidType(newType)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if (!newType.equalsIgnoreCase(vehicle.getType())) {
                    Vaga associatedSpot = vagaDAO.getVagaByLicensePlate(licensePlate);
                    if (associatedSpot != null) {
                        res.addProperty("statusCode", 409);
                        res.addProperty("message", "Veículo associado a uma vaga. Libere a vaga antes desta operação.");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                }
            }

            vehicle.setBrand(newBrand);
            vehicle.setModel(newModel);
            vehicle.setType(newType);

            vehicleDAO.updateVehicle(vehicle);
            loadAllVehicles();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo atualizado com sucesso");
            res.add("data", JsonNull.INSTANCE);
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

    public synchronized JsonObject deleteVehicle(JsonObject req) {
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

            String token = dataObj.get("token").getAsString().trim();
            String licensePlate = dataObj.get("licensePlate").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());

            if (vehicle == null || (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername()))) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga associatedSpot = vagaDAO.getVagaByLicensePlate(licensePlate);
            if (associatedSpot != null) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Veículo associado a uma vaga. Libere a vaga antes desta operação.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            vehicleDAO.deleteVehicle(licensePlate);
            loadAllVehicles();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veículo removido com sucesso");
            res.add("data", JsonNull.INSTANCE);
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

    private boolean isValidLicensePlate(String plate) {
        return plate != null && plate.length() == 7;
    }

    private boolean isValidBrand(String brand) {
        return brand != null && brand.length() >= 1 && brand.length() <= 20;
    }

    private boolean isValidType(String type) {
        return "eletrico".equals(type) || "combustao".equals(type);
    }
}
