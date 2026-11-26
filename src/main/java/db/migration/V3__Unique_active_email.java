package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V3__Unique_active_email extends BaseJavaMigration {

    private static final String POSTGRESQL = "PostgreSQL";

    @Override
    public void migrate(Context context) throws Exception {
        String product = context.getConnection().getMetaData().getDatabaseProductName();
        if (!POSTGRESQL.equals(product)) {
            return;
        }
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("CREATE UNIQUE INDEX ux_client_email_active ON client (LOWER(email)) WHERE deleted = FALSE");
            statement.execute("CREATE UNIQUE INDEX ux_employee_email_active ON employee (LOWER(email)) WHERE deleted = FALSE");
        }
    }
}
