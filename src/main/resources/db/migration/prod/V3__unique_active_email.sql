CREATE UNIQUE INDEX ux_client_email_active   ON client   (LOWER(email)) WHERE deleted = FALSE;
CREATE UNIQUE INDEX ux_employee_email_active ON employee (LOWER(email)) WHERE deleted = FALSE;
