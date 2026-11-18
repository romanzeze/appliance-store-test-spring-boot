package db.migration.common;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.util.Map;

public class V2__Create_initial_admin extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, String> placeholders = context.getConfiguration().getPlaceholders();
        String email = placeholders.get("adminEmail");
        String password = placeholders.get("adminPassword");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL and ADMIN_INITIAL_PASSWORD must be set to create the initial admin account");
        }
        String hash = new BCryptPasswordEncoder().encode(password);
        String sql = "INSERT INTO employee (name, email, password, department, role, must_change_password) "
                + "VALUES (?, ?, ?, ?, 'ADMIN', TRUE)";
        try (PreparedStatement ps = context.getConnection().prepareStatement(sql)) {
            ps.setString(1, "Administrator");
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, "administration");
            ps.executeUpdate();
        }
    }
}
