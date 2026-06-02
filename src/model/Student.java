package model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final IntegerProperty id;
    private final StringProperty roll;
    private final StringProperty name;
    private final StringProperty department;
    private final StringProperty email;
    private final StringProperty phone;
    private final StringProperty course;
    private final StringProperty semester;
    private final IntegerProperty marks;

    public Student() {
        this(0, "", "", "");
    }

    public Student(int id, String roll, String name, String department) {
        this.id = new SimpleIntegerProperty(id);
        this.roll = new SimpleStringProperty(roll != null ? roll : "");
        this.name = new SimpleStringProperty(name != null ? name : "");
        this.department = new SimpleStringProperty(department != null ? department : "");
        this.email = new SimpleStringProperty("");
        this.phone = new SimpleStringProperty("");
        this.course = new SimpleStringProperty("");
        this.semester = new SimpleStringProperty("");
        this.marks = new SimpleIntegerProperty(0);
    }

    public Student(int id, String roll, String name, String department,
                  String email, String phone, String course,
                  String semester, int marks) {
        this.id = new SimpleIntegerProperty(id);
        this.roll = new SimpleStringProperty(roll != null ? roll : "");
        this.name = new SimpleStringProperty(name != null ? name : "");
        this.department = new SimpleStringProperty(department != null ? department : "");
        this.email = new SimpleStringProperty(email != null ? email : "");
        this.phone = new SimpleStringProperty(phone != null ? phone : "");
        this.course = new SimpleStringProperty(course != null ? course : "");
        this.semester = new SimpleStringProperty(semester != null ? semester : "");
        this.marks = new SimpleIntegerProperty(marks);
    }

    // ---- id ----
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    // ---- roll ----
    public String getRoll() { return roll.get(); }
    public void setRoll(String roll) { this.roll.set(roll); }
    public StringProperty rollProperty() { return roll; }

    // ---- name ----
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    // ---- department ----
    public String getDepartment() { return department.get(); }
    public void setDepartment(String department) { this.department.set(department); }
    public StringProperty departmentProperty() { return department; }

    // ---- email ----
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public StringProperty emailProperty() { return email; }

    // ---- phone ----
    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public StringProperty phoneProperty() { return phone; }

    // ---- course ----
    public String getCourse() { return course.get(); }
    public void setCourse(String course) { this.course.set(course); }
    public StringProperty courseProperty() { return course; }

    // ---- semester ----
    public String getSemester() { return semester.get(); }
    public void setSemester(String semester) { this.semester.set(semester); }
    public StringProperty semesterProperty() { return semester; }

    // ---- marks ----
    public int getMarks() { return marks.get(); }
    public void setMarks(int marks) { this.marks.set(marks); }
    public IntegerProperty marksProperty() { return marks; }

    @Override
    public String toString() {
        return roll.get() + " - " + name.get();
    }
}