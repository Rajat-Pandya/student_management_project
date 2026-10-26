# Student Management System

<div align="center">

![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange?logo=java)
![Database](https://img.shields.io/badge/Database-MySQL%2FSQLite-green?logo=mysql)
![License](https://img.shields.io/badge/License-MIT-yellow)

**A modern, professional JavaFX desktop application for comprehensive educational institution management.**

[Features](#features) • [Installation](#installation) • [Screenshots](#screenshots) • [Architecture](#architecture)

</div>

---

## Project Overview

The **Student Management System** is a feature-rich desktop application built with **JavaFX** that provides a complete solution for managing students, faculty, subjects, attendance, and academic records in educational institutions.

### Key Highlights

- **Modern UI/UX** - Clean, responsive interface with professional styling
- **Role-Based Access** - Separate dashboards for Admin and Faculty users
- **Real-time Analytics** - Dashboard with statistics and data visualization
- **Secure Authentication** - BCrypt password hashing with automatic rehashing
- **Export Capabilities** - PDF reports and CSV data export
- **Multi-Database Support** - Works with MySQL or SQLite

---

## Features

### Admin Dashboard
- **Student Management** - Add, edit, delete, and search students with validation
- **Faculty Management** - Manage faculty accounts and assignments
- **Subject Management** - Configure subjects per semester and department
- **Subject Assignment** - Assign subjects to faculty for specific semesters
- **Database Backup** - Export all tables to CSV format
- **User Management** - Create additional admin accounts

### Faculty Dashboard
- **Student Attendance** - Mark daily attendance with subject selection
- **Marks Entry** - Record student marks for assigned subjects
- **Grade Assignment** - Auto-calculate and assign grades (A+ through F)
- **Attendance Reports** - View and filter attendance records
- **Marksheet Generation** - Generate and export student marksheets to PDF

### System Features
- **Secure Authentication** - BCrypt password hashing with role-based access
- **Session Management** - Global session tracking for logged-in users
- **Responsive UI** - Scalable layouts using CSS and JavaFX bindings
- **Data Validation** - Input validation and duplicate detection
- **Error Handling** - Graceful error handling and user notifications

---

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 17+ |
| **GUI Framework** | JavaFX 21 |
| **Database** | MySQL 8.0+ / SQLite |
| **Password Hashing** | BCrypt (jBCrypt) |
| **PDF Export** | Apache PDFBox |
| **Architecture** | MVC Pattern |
| **Build** | Eclipse / IntelliJ IDEA |

---

## Installation

### Prerequisites

- **Java Development Kit (JDK)** 17 or higher
- **MySQL** 8.0+ (or use SQLite for local development)
- **MySQL Connector/J** (for MySQL support)
- **JavaFX SDK** 21
- **BCrypt Library** (jBCrypt)
- **Apache PDFBox** (for PDF export)

### Database Setup

#### Option 1: MySQL

1. Create a MySQL database:
```sql
CREATE DATABASE student_db;
USE student_db;
```

2. Execute the schema script:
```bash
mysql -u root -p student_db < database/schema.sql
```

3. Configure connection (optional - uses defaults):
```bash
set DB_URL=jdbc:mysql://localhost:3306/student_db
set DB_USER=root
set DB_PASSWORD=your_password
```

#### Option 2: SQLite (Default)

No setup required! The application will automatically create `student_db.db` in the project root.

### Building & Running

#### Using Eclipse

1. Import the project into Eclipse
2. Add JavaFX library to the build path
3. Add MySQL Connector/J JAR (if using MySQL)
4. Add jBCrypt and PDFBox JARs to the build path
5. Run `src/controller/Main.java`

#### Using Command Line

```bash
# Compile
cd src
javac -d ../bin -cp "../lib/*" controller/Main.java

# Run
cd ..
java -cp "bin;lib/*" controller.Main
```

### Default Login Credentials

> [!WARNING]
> **Demo Account Notice:** The default admin account (`admin` / `demo_admin_change_me`) is provided for local demonstration and evaluation only. This credential must be changed immediately before any real or production use.

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | demo_admin_change_me |
| Faculty | (create via admin) | (set during creation) |

---

## Project Structure

```
student_management_project/
├── src/
│   ├── controller/          # JavaFX Controllers
│   │   ├── Main.java
│   │   ├── LoginController.java
│   │   ├── AdminController.java
│   │   ├── AdminHomeController.java
│   │   ├── StudentsController.java
│   │   ├── ManageFacultyController.java
│   │   ├── ManageSubjectsController.java
│   │   ├── AssignSubjectsController.java
│   │   ├── FacultyDashboardController.java
│   │   ├── FacultyHomeController.java
│   │   ├── FacultyStudentsController.java
│   │   ├── AssignMarksController.java
│   │   ├── AssignGradesController.java
│   │   ├── ViewAttendanceController.java
│   │   ├── ViewMarksheetController.java
│   │   └── UIUtils.java
│   ├── model/               # Domain Models
│   │   ├── Student.java
│   │   ├── FacultyModel.java
│   │   ├── Subject.java
│   │   ├── User.java
│   │   └── FacultySubjectAssignment.java
│   ├── dao/                 # Data Access Objects
│   │   ├── StudentDAO.java
│   │   ├── FacultyDAO.java
│   │   ├── SubjectDAO.java
│   │   ├── UserDAO.java
│   │   ├── FacultySubjectDAO.java
│   │   ├── MarksDAO.java
│   │   └── AttendanceDAO.java
│   └── util/                # Utilities
│       ├── DatabaseConnection.java
│       ├── PasswordUtils.java
│       ├── AuthService.java
│       └── Session.java
├── resources/
│   ├── fxml/               # JavaFX Layout Files
│   │   ├── Login.fxml
│   │   ├── AdminDashboard.fxml
│   │   ├── AdminHome.fxml
│   │   ├── ManageStudents.fxml
│   │   ├── ManageFaculty.fxml
│   │   ├── ManageSubjects.fxml
│   │   ├── AssignSubjects.fxml
│   │   ├── FacultyDashboard.fxml
│   │   ├── FacultyHome.fxml
│   │   ├── FacultyStudents.fxml
│   │   ├── AssignMarks.fxml
│   │   ├── AssignGrades.fxml
│   │   ├── ViewAttendance.fxml
│   │   └── ViewMarksheet.fxml
│   ├── css/                # Stylesheets
│   │   └── styles.css
│   └── images/             # Image Assets
│       └── logo.jpg
├── database/
│   └── schema.sql          # Database Schema
├── screenshots/            # Application Screenshots
├── docs/                   # Documentation
├── .gitignore
└── README.md
```

---

## Architecture

The application follows the **Model-View-Controller (MVC)** architectural pattern:

### Model Layer
- `Student`, `FacultyModel`, `Subject`, `User` - Domain entities with JavaFX properties
- Encapsulates data and business logic

### View Layer
- FXML files for declarative UI layouts
- CSS stylesheets for consistent theming
- Responsive design with JavaFX bindings

### Controller Layer
- Handle user interactions and input validation
- Coordinate between View and Model layers
- Manage scene transitions and navigation

### Data Access Layer
- DAO classes for database operations
- Prepared statements for SQL injection prevention
- Connection pooling and transaction management

### Utility Layer
- `DatabaseConnection` - Database connection factory
- `PasswordUtils` - BCrypt password handling
- `AuthService` - Authentication logic
- `Session` - Global session management

---

## Screenshots

<div align="center">

### Login Screen
![Login](screenshots/login.png)

### Admin Dashboard
![Admin Dashboard](screenshots/admin_dashboard.png)

### Student Management
![Student Management](screenshots/student_management.png)

### Faculty Dashboard
![Faculty Dashboard](screenshots/faculty_dashboard.png)

### Attendance Module
![Attendance](screenshots/attendance.png)

</div>

---

## Security Features

- **Password Hashing** - BCrypt with automatic cost factor upgrades
- **SQL Injection Prevention** - Prepared statements throughout
- **Role-Based Access Control** - Separate interfaces for admin and faculty
- **Session Management** - Secure session handling with proper cleanup
- **Input Validation** - Client-side and server-side validation

---

## Future Improvements

- [ ] REST API integration for mobile apps
- [ ] Email notifications for attendance/grades
- [ ] Advanced analytics with charts
- [ ] Student portal for self-service
- [ ] Multi-language support
- [ ] Dark/Light theme toggle
- [ ] Database migration tool
- [ ] Automated testing suite

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## Author

**Your Name**
- GitHub: [@yourusername](https://github.com/yourusername)
- LinkedIn: [Your LinkedIn](https://linkedin.com/in/yourprofile)
- Email: your.email@example.com

---

## Acknowledgments

- JavaFX Documentation and Community
- MySQL and SQLite Communities
- University project reference and guidance
- Icons and design inspiration from various open-source projects

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

</div>
