package service;

import java.sql.SQLException;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import dao.VagaDAO;
import dao.VehicleDAO;
import entities.Session;
import entities.User;
import entities.Vaga;
import entities.Vehicle;

public class VagaService {

    private final VagaDAO vagaDAO = new VagaDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final UserService userService = new UserService();

    public synchronized JsonObject createVaga(JsonObject req) {
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

            if (!"admin".equalsIgnoreCase(loggedUser.getRole())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!hasNonBlankString(dataObj, "spotName") || !hasNonBlankString(dataObj, "type")
                    || !dataObj.has("floor") || dataObj.get("floor").isJsonNull()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String spotName = dataObj.get("spotName").getAsString().trim();
            String type = dataObj.get("type").getAsString().trim();

            if (!isValidSpotName(spotName) || !isValidType(type)) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            int floor;
            try {
                if (!dataObj.get("floor").isJsonPrimitive() || !dataObj.get("floor").getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException();
                }
                floor = dataObj.get("floor").getAsInt();
            } catch (Exception e) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga existing = vagaDAO.getVagaBySpotName(spotName);
            if (existing != null) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Vaga já existe.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga vaga = new Vaga();
            vaga.setSpotName(spotName);
            vaga.setFloor(floor);
            vaga.setType(type);
            vaga.setStatus("disponivel");
            vaga.setLicensePlate(null);

            vagaDAO.createVaga(vaga);
            loadAllVagas();

            res.addProperty("statusCode", 201);
            res.addProperty("message", "Vaga criada com sucesso");
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

    public JsonObject getVagas(JsonObject req) {
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

            List<Vaga> vagas = vagaDAO.getAllVagas();
            JsonArray dataArray = new JsonArray();

            for (Vaga vaga : vagas) {
                JsonObject obj = new JsonObject();
                obj.addProperty("floor", vaga.getFloor());
                obj.addProperty("spotName", vaga.getSpotName());
                obj.addProperty("status", vaga.getStatus());
                if (vaga.getLicensePlate() == null) {
                    obj.add("licensePlate", JsonNull.INSTANCE);
                } else {
                    obj.addProperty("licensePlate", vaga.getLicensePlate());
                }
                obj.addProperty("type", vaga.getType());
                dataArray.add(obj);
            }

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Vagas obtidas com sucesso");
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

    public synchronized JsonObject updateVaga(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "spotName")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString().trim();
            String spotName = dataObj.get("spotName").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga vaga = vagaDAO.getVagaBySpotName(spotName);
            if (vaga == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Vaga não encontrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());

            if (dataObj.has("newSpotName") && !dataObj.get("newSpotName").isJsonNull()) {
                if (!isAdmin) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                String newSpotName = dataObj.get("newSpotName").getAsString().trim();
                if (!isValidSpotName(newSpotName)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                if (!newSpotName.equals(spotName)) {
                    Vaga conflict = vagaDAO.getVagaBySpotName(newSpotName);
                    if (conflict != null) {
                        res.addProperty("statusCode", 409);
                        res.addProperty("message", "Já existe uma vaga com o nome informado.");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                    vaga.setSpotName(newSpotName);
                }
            }

            if (dataObj.has("floor") && !dataObj.get("floor").isJsonNull()) {
                if (!isAdmin) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                int newFloor;
                try {
                    if (!dataObj.get("floor").isJsonPrimitive() || !dataObj.get("floor").getAsJsonPrimitive().isNumber()) {
                        throw new IllegalArgumentException();
                    }
                    newFloor = dataObj.get("floor").getAsInt();
                } catch (Exception e) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vaga.setFloor(newFloor);
            }

            if (dataObj.has("type") && !dataObj.get("type").isJsonNull()) {
                String newType = dataObj.get("type").getAsString().trim();
                if (!isValidType(newType)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                if (!newType.equals(vaga.getType())) {
                    if (!isAdmin) {
                        res.addProperty("statusCode", 401);
                        res.addProperty("message", "Operação não autorizada.");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                    vaga.setType(newType);
                }
            }

            boolean explicitNullPlate = dataObj.has("licensePlate") && dataObj.get("licensePlate").isJsonNull();
            boolean hasStatusField = dataObj.has("status") && !dataObj.get("status").isJsonNull();

            String targetStatus = vaga.getStatus();
            if (hasStatusField) {
                targetStatus = dataObj.get("status").getAsString().trim();
                if (!isValidStatus(targetStatus)) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
            } else if (explicitNullPlate) {
                targetStatus = "disponivel";
            } else if (hasNonBlankString(dataObj, "licensePlate")) {
                targetStatus = "ocupada";
            }

            if ("fora_de_servico".equals(targetStatus)) {
                if (!isAdmin) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                if (!"disponivel".equalsIgnoreCase(vaga.getStatus()) && !"fora_de_servico".equalsIgnoreCase(vaga.getStatus())) {
                    res.addProperty("statusCode", 409);
                    res.addProperty("message", "A vaga só pode ser alterada para fora de serviço se estiver disponível.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vaga.setStatus("fora_de_servico");
                vaga.setLicensePlate(null);
            } else if ("disponivel".equals(targetStatus)) {
                if (!isAdmin && "ocupada".equalsIgnoreCase(vaga.getStatus()) && vaga.getLicensePlate() != null) {
                    Vehicle currentVehicle = vehicleDAO.getVehicleByLicensePlate(vaga.getLicensePlate());
                    if (currentVehicle == null || !currentVehicle.getUsername().equals(loggedUser.getUsername())) {
                        res.addProperty("statusCode", 401);
                        res.addProperty("message", "Operação não autorizada.");
                        res.add("data", JsonNull.INSTANCE);
                        return res;
                    }
                }
                if (!isAdmin && "fora_de_servico".equalsIgnoreCase(vaga.getStatus())) {
                    res.addProperty("statusCode", 401);
                    res.addProperty("message", "Operação não autorizada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }
                vaga.setStatus("disponivel");
                vaga.setLicensePlate(null);
            } else if ("ocupada".equals(targetStatus)) {
                String plateToUse = vaga.getLicensePlate();
                if (hasNonBlankString(dataObj, "licensePlate")) {
                    plateToUse = dataObj.get("licensePlate").getAsString().trim();
                } else if (explicitNullPlate) {
                    plateToUse = null;
                }

                if (plateToUse == null || plateToUse.isEmpty()) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if ("fora_de_servico".equalsIgnoreCase(vaga.getStatus())) {
                    res.addProperty("statusCode", 409);
                    res.addProperty("message", "Vaga fora de serviço.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if ("ocupada".equalsIgnoreCase(vaga.getStatus())
                        && vaga.getLicensePlate() != null && !vaga.getLicensePlate().equalsIgnoreCase(plateToUse)) {
                    res.addProperty("statusCode", 409);
                    res.addProperty("message", "Vaga já está ocupada.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(plateToUse);
                if (vehicle == null) {
                    res.addProperty("statusCode", 404);
                    res.addProperty("message", "Veículo não encontrado.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername())) {
                    res.addProperty("statusCode", 404);
                    res.addProperty("message", "Veículo não encontrado.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                if (!vehicle.getType().equalsIgnoreCase(vaga.getType())) {
                    res.addProperty("statusCode", 400);
                    res.addProperty("message", "Tipo do veículo incompatível com o tipo da vaga.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                Vaga parkedSpot = vagaDAO.getVagaByLicensePlate(vehicle.getLicensePlate());
                if (parkedSpot != null && !parkedSpot.getSpotName().equals(spotName)) {
                    res.addProperty("statusCode", 409);
                    res.addProperty("message", "Veículo já está estacionado em outra vaga.");
                    res.add("data", JsonNull.INSTANCE);
                    return res;
                }

                vaga.setStatus("ocupada");
                vaga.setLicensePlate(vehicle.getLicensePlate());
            }

            vagaDAO.updateVaga(spotName, vaga);
            loadAllVagas();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Vaga atualizada com sucesso");
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

    public synchronized JsonObject deleteVaga(JsonObject req) {
        JsonObject res = new JsonObject();
        try {
            if (req == null || !req.has("data") || req.get("data").isJsonNull() || !req.get("data").isJsonObject()) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject dataObj = req.get("data").getAsJsonObject();
            if (!hasNonBlankString(dataObj, "token") || !hasNonBlankString(dataObj, "spotName")) {
                res.addProperty("statusCode", 400);
                res.addProperty("message", "Campos obrigatórios ausentes ou inválidos.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            String token = dataObj.get("token").getAsString().trim();
            String spotName = dataObj.get("spotName").getAsString().trim();

            User loggedUser = userService.getAuthenticatedUser(token);
            if (loggedUser == null) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Sessão expirada ou encerrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if (!"admin".equalsIgnoreCase(loggedUser.getRole())) {
                res.addProperty("statusCode", 401);
                res.addProperty("message", "Operação não autorizada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga vaga = vagaDAO.getVagaBySpotName(spotName);
            if (vaga == null) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Vaga não encontrada.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            if ("ocupada".equalsIgnoreCase(vaga.getStatus()) || vaga.getLicensePlate() != null) {
                res.addProperty("statusCode", 409);
                res.addProperty("message", "Vagas ocupadas não podem ser excluídas.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            vagaDAO.deleteVaga(spotName);
            loadAllVagas();

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Vaga removida com sucesso");
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

    public JsonObject findVehicle(JsonObject req) {
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

            boolean isAdmin = "admin".equalsIgnoreCase(loggedUser.getRole());
            Vehicle vehicle = vehicleDAO.getVehicleByLicensePlate(licensePlate);
            if (vehicle == null || (!isAdmin && !vehicle.getUsername().equals(loggedUser.getUsername()))) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            Vaga vaga = vagaDAO.getVagaByLicensePlate(licensePlate);
            if (vaga == null || !"ocupada".equalsIgnoreCase(vaga.getStatus())) {
                res.addProperty("statusCode", 404);
                res.addProperty("message", "Veículo não encontrado.");
                res.add("data", JsonNull.INSTANCE);
                return res;
            }

            JsonObject spotData = new JsonObject();
            spotData.addProperty("floor", vaga.getFloor());
            spotData.addProperty("spotName", vaga.getSpotName());
            spotData.addProperty("status", vaga.getStatus());

            res.addProperty("statusCode", 200);
            res.addProperty("message", "Veiculo localizado com sucesso");
            res.add("data", spotData);
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

    public void loadAllVagas() {
        try {
            List<Vaga> vagas = vagaDAO.getAllVagas();
            if (vagas != null) {
                Session.setVagas(vagas);
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar vagas: " + e.getMessage());
        }
    }

    private boolean hasNonBlankString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull() && !obj.get(key).getAsString().trim().isEmpty();
    }

    private boolean isValidSpotName(String spotName) {
        return spotName != null && spotName.length() >= 1 && spotName.length() <= 4;
    }

    private boolean isValidType(String type) {
        return "eletrico".equals(type) || "combustao".equals(type);
    }

    private boolean isValidStatus(String status) {
        return "disponivel".equals(status) || "fora_de_servico".equals(status) || "ocupada".equals(status);
    }
}
