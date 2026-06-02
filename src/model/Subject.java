package model;

public class Subject {
    private int id;
    private String code;
    private String name;
    private int semester;
    private String department;

    public Subject() {}

    public Subject(String code, String name, int semester) {
        this.code = code;
        this.name = name;
        this.semester = semester;
    }

    public Subject(String code, String name, int semester, String department) {
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.department = department;
    }

    public Subject(int id, String code, String name, int semester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.semester = semester;
    }

    public Subject(int id, String code, String name, int semester, String department) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.semester = semester;
        this.department = department;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    // 👇 Add this method to fix ComboBox text
    @Override
    public String toString() {
        // You can adjust this format to your needs
        return code + " - " + name;  
        // Or: return name;  (if you just want the subject name)
        // Or: return name + " (Sem " + semester + ")"; 
    }
}
