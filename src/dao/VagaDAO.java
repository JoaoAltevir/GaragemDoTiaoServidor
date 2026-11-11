package dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import entities.Vaga;

public class VagaDAO {

    public void createVaga(Vaga vaga) throws SQLException {
        String sql = "INSERT INTO vaga (spot_name, floor, type, status, license_plate) VALUES (?, ?, ?, ?, ?)";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, vaga.getSpotName());
            stmt.setInt(2, vaga.getFloor());
            stmt.setString(3, vaga.getType());
            stmt.setString(4, vaga.getStatus());
            if (vaga.getLicensePlate() == null || vaga.getLicensePlate().isEmpty()) {
                stmt.setNull(5, Types.VARCHAR);
            } else {
                stmt.setString(5, vaga.getLicensePlate());
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public Vaga getVagaBySpotName(String spotName) throws SQLException {
        String sql = "SELECT * FROM vaga WHERE spot_name = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, spotName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVaga(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public List<Vaga> getAllVagas() throws SQLException {
        String sql = "SELECT * FROM vaga";
        List<Vaga> vagas = new ArrayList<>();
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                vagas.add(mapResultSetToVaga(rs));
            }
            return vagas;
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public Vaga getVagaByLicensePlate(String licensePlate) throws SQLException {
        String sql = "SELECT * FROM vaga WHERE license_plate = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, licensePlate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVaga(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public boolean hasParkedVehicleForUser(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vaga v INNER JOIN veiculo ve ON v.license_plate = ve.license_plate WHERE ve.username = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public int updateVaga(String currentSpotName, Vaga vaga) throws SQLException {
        String sql = "UPDATE vaga SET spot_name = ?, floor = ?, type = ?, status = ?, license_plate = ? WHERE spot_name = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, vaga.getSpotName());
            stmt.setInt(2, vaga.getFloor());
            stmt.setString(3, vaga.getType());
            stmt.setString(4, vaga.getStatus());
            if (vaga.getLicensePlate() == null || vaga.getLicensePlate().isEmpty()) {
                stmt.setNull(5, Types.VARCHAR);
            } else {
                stmt.setString(5, vaga.getLicensePlate());
            }
            stmt.setString(6, currentSpotName);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    public int deleteVaga(String spotName) throws SQLException {
        String sql = "DELETE FROM vaga WHERE spot_name = ?";
        try (
            Connection conn = BancoDados.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, spotName);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (IOException e) {
            throw new SQLException("Erro ao conectar ao banco de dados", e);
        }
    }

    private Vaga mapResultSetToVaga(ResultSet rs) throws SQLException {
        Vaga vaga = new Vaga();
        vaga.setSpotName(rs.getString("spot_name"));
        vaga.setFloor(rs.getInt("floor"));
        vaga.setType(rs.getString("type"));
        vaga.setStatus(rs.getString("status"));
        vaga.setLicensePlate(rs.getString("license_plate"));
        vaga.setCreatedAt(rs.getTimestamp("created_at"));
        vaga.setUpdatedAt(rs.getTimestamp("updated_at"));
        return vaga;
    }
}
