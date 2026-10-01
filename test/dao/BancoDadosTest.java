package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

public class BancoDadosTest {

	@Test
	public void conectarTeste() throws SQLException, IOException {

		Connection conn = BancoDados.conectar();
		assertNotNull(conn);
	}

}