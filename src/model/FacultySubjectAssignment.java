package model;

import java.time.LocalDate;

public class FacultySubjectAssignment {
    private final int id;
    private final int facultyId;
    private final String facultyName;
    private final int subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final int semester;
    private final LocalDate assignedAt;

    public FacultySubjectAssignment(int id, int facultyId, String facultyName, int subjectId, String subjectCode, String subjectName, int semester, LocalDate assignedAt) {
        this.id = id;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.semester = semester;
        this.assignedAt = assignedAt;
    }

    public int getId() { return id; }
    public int getFacultyId() { return facultyId; }
    public String getFacultyName() { return facultyName; }
    public int getSubjectId() { return subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public String getSubjectName() { return subjectName; }
    public int getSemester() { return semester; }
    public LocalDate getAssignedAt() { return assignedAt; }
}
