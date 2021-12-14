package org.cloudfoundry.credhub.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;

import java.sql.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FlywayMigrationStrategyConfigurationTest {
    private Connection mockConnection;
    private Flyway mockFlyway;
    private DatabaseMetaData mockDatabaseMetaData;
    private Statement mockStatement;
    private ResultSet mockResultSetLegacyTable;
    private ResultSet mockResultSetNewTable;
    private ResultSet mockResultSetBackupTable;

    @Before
    public void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        mockFlyway = mock(Flyway.class);
        mockDatabaseMetaData = mock(DatabaseMetaData.class);
        mockStatement = mock(Statement.class);
        mockResultSetLegacyTable = mock(ResultSet.class);
        mockResultSetNewTable = mock(ResultSet.class);
        mockResultSetBackupTable = mock(ResultSet.class);
        Configuration mockConfiguration = mock(Configuration.class);
        DataSource mockDataSource = mock(DataSource.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConfiguration.getDataSource()).thenReturn(mockDataSource);
        when(mockFlyway.getConfiguration()).thenReturn(mockConfiguration);
        when(mockStatement.execute(any())).thenReturn(false);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
    }

    @Test
    public void doesNotRenameIfOnlyNewTableExists() throws SQLException {
        mockTableExistenceInDB("schema_version", false, mockResultSetLegacyTable, mockDatabaseMetaData);
        mockTableExistenceInDB("flyway_schema_history", true, mockResultSetNewTable, mockDatabaseMetaData);
        mockTableExistenceInDB("backup_schema_version", false, mockResultSetBackupTable, mockDatabaseMetaData);
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);

        new FlywayMigrationStrategyConfiguration().renameMigrationTableIfNeeded(mockFlyway);

        verify(mockStatement, times(0)).execute(any());
    }

    @Test
    public void renameLegacyTableToNewIfOnlyLegacyTableExists() throws SQLException {
        mockTableExistenceInDB("schema_version", true, mockResultSetLegacyTable, mockDatabaseMetaData);
        mockTableExistenceInDB("flyway_schema_history", false, mockResultSetNewTable, mockDatabaseMetaData);
        mockTableExistenceInDB("backup_schema_version", false, mockResultSetBackupTable, mockDatabaseMetaData);
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);

        new FlywayMigrationStrategyConfiguration().renameMigrationTableIfNeeded(mockFlyway);

        verify(mockStatement, times(1)).execute(eq("ALTER TABLE schema_version RENAME TO flyway_schema_history"));
    }

    @Test
    public void renameLegacyTableToBackupIfBothNewAndLegacyTablesExist() throws SQLException {
        mockTableExistenceInDB("schema_version", true, mockResultSetLegacyTable, mockDatabaseMetaData);
        mockTableExistenceInDB("flyway_schema_history", true, mockResultSetNewTable, mockDatabaseMetaData);
        mockTableExistenceInDB("backup_schema_version", false, mockResultSetBackupTable, mockDatabaseMetaData);
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);

        new FlywayMigrationStrategyConfiguration().renameMigrationTableIfNeeded(mockFlyway);

        verify(mockStatement, times(1)).execute(eq("ALTER TABLE schema_version RENAME TO backup_schema_version"));
    }

    @Test
    public void doesNotRenameIfNewOldBackUpTableAllExist() throws SQLException {
        mockTableExistenceInDB("schema_version", true, mockResultSetLegacyTable, mockDatabaseMetaData);
        mockTableExistenceInDB("flyway_schema_history", true, mockResultSetNewTable, mockDatabaseMetaData);
        mockTableExistenceInDB("backup_schema_version", true, mockResultSetBackupTable, mockDatabaseMetaData);
        when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);

        new FlywayMigrationStrategyConfiguration().renameMigrationTableIfNeeded(mockFlyway);

        verify(mockStatement, times(0)).execute(any());
    }

    private void mockTableExistenceInDB(String tableName, boolean tableExists, ResultSet mockResultSetLegacyTable, DatabaseMetaData mockDatabaseMetaData) throws SQLException {
        when(mockResultSetLegacyTable.next()).thenReturn(tableExists);
        when(mockDatabaseMetaData.getTables(isNull(), isNull(), eq(tableName), any())).thenReturn(mockResultSetLegacyTable);
    }
}