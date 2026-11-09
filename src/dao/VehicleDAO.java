package dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import entities.Vehicle;

public class VehicleDAO {

    public void createVehicle(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO veiculo (license_plate, brand, model, type, username) VALUES (?, ?, ?, ?, ?)";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, vehicle.getLicensePlate());
            stmt.setString(2, vehicle.getBrand());
            stmt.setString(3, vehicle.getModel());
            stmt.setString(4, vehicle.getType());
            stmt.setString(5, vehicle.getUsername());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public Vehicle getVehicleByLicensePlate(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM veiculo WHERE license_plate = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, licensePlate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicle(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public List<Vehicle> getVehiclesByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM veiculo WHERE username = ?";
        List<Vehicle> vehicles = new ArrayList<>();
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicle(rs));
                }
                return vehicles;
            }
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public List<Vehicle> getAllVehicles() throws SQLException {
        String sql = "SELECT * FROM veiculo";
        List<Vehicle> vehicles = new ArrayList<>();
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                vehicles.add(mapResultSetToVehicle(rs));
            }
            return vehicles;
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public int updateVehicle(Vehicle vehicle) throws SQLException {
        String sql = "UPDATE veiculo SET brand = ?, model = ?, type = ? WHERE license_plate = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, vehicle.getBrand());
            stmt.setString(2, vehicle.getModel());
            stmt.setString(3, vehicle.getType());
            stmt.setString(4, vehicle.getLicensePlate());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public int deleteVehicle(String licensePlate) throws SQLException {
        String sqlFreeSpot = "UPDATE vaga SET status = 'disponivel', license_plate = NULL WHERE license_plate = ?";
        String sqlDelete = "DELETE FROM veiculo WHERE license_plate = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmtFreeSpot = conn.prepareStatement(sqlFreeSpot);
            PreparedStatement stmtDelete = conn.prepareStatement(sqlDelete);
        ) {
            stmtFreeSpot.setString(1, licensePlate);
            stmtFreeSpot.executeUpdate();

            stmtDelete.setString(1, licensePlate);
            return stmtDelete.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    private Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(rs.getString("license_plate"));
        vehicle.setBrand(rs.getString("brand"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setType(rs.getString("type"));
        vehicle.setUsername(rs.getString("username"));
        vehicle.setCreatedAt(rs.getTimestamp("created_at"));
        vehicle.setUpdatedAt(rs.getTimestamp("updated_at"));
        return vehicle;
    }
}
