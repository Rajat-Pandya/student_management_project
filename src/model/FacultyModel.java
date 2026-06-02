package model;

import javafx.beans.property.*;

/**
 * JavaFX model class for Faculty entity.
 * 
 * Provides both property getters (for binding in TableView/ComboBox) 
 * and standard getters/setters (for DAO usage).
 */
public class FacultyModel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty department = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();

    public FacultyModel(int id, String name, String email, String username,
                        String password, String phone, String department, String role) {
        this.id.set(id);
        this.name.set(name);
        this.email.set(email);
        this.username.set(username);
        this.password.set(password);
        this.phone.set(phone);
        this.department.set(department);
        this.role.set(role);
    }

    // --- id
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // --- name
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    // --- email
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public StringProperty emailProperty() { return email; }

    // --- username
    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }
    public StringProperty usernameProperty() { return username; }

    // --- password
    public String getPassword() { return password.get(); }
    public void setPassword(String password) { this.password.set(password); }
    public StringProperty passwordProperty() { return password; }

    // --- phone
    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public StringProperty phoneProperty() { return phone; }

    // --- department
    public String getDepartment() { return department.get(); }
    public void setDepartment(String department) { this.department.set(department); }
    public StringProperty departmentProperty() { return department; }

    // --- role
    public String getRole() { return role.get(); }
    public void setRole(String role) { this.role.set(role); }
    public StringProperty roleProperty() { return role; }

    @Override
    public String toString() {
        return String.format("%s (%s)", getName(), getDepartment());
    }
}
