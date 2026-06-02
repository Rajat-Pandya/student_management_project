# Student Management System - Technical Documentation

## Project Overview

This is a comprehensive JavaFX-based Student Management System designed for educational institutions. The system provides role-based access for administrators and faculty members, with features for student management, attendance tracking, marks entry, and reporting.

---

## Architecture

### MVC Pattern

The application follows the Model-View-Controller (MVC) architectural pattern:

- **Model** (`src/model/`): Domain objects representing entities
- **View** (`resources/fxml/`): JavaFX FXML layouts
- **Controller** (`src/controller/`): Handles user interactions
- **DAO** (`src/dao/`): Data Access Objects for database operations
- **Utility** (`src/util/`): Helper classes and services

---

## Database Schema

### Tables

1. **students**: Stores student information
   - id, roll_number, name, course, semester, email, phone, created_at

2. **faculty**: Stores faculty information
   - id, name, username, password, email, phone, department, role

3. **subjects**: Stores subject information
   - id, code, name, semester, department

4. **users**: Stores admin user accounts
   - id, username, password, role, department, email, created_at

5. **marks**: Stores student marks
   - id, student_id, subject_id, marks, grade, semester

6. **attendance**: Stores attendance records
   - id, student_id, subject_id, date, status

7. **faculty_subjects**: Links faculty to subjects
   - id, faculty_id, subject_id, semester

---

## Security Features

### Authentication
- BCrypt password hashing with automatic rehashing
- Role-based access control (Admin/Faculty)
- Secure session management

### Database Security
- Prepared statements for all queries
- SQL injection prevention
- Connection pooling

---

## Utility Classes

### AnalyticsUtil
Comprehensive analytics and statistics generation:
- Student statistics
- Attendance analytics
- Marks statistics
- Faculty statistics
- Performance trends

### ValidationUtil
Input validation and sanitization:
- Email validation
- Phone number validation
- Name validation
- Password strength checking
- Custom validation rules

### NotificationUtil
Modern notification system:
- Toast notifications
- Success/error/warning/info messages
- Persistent notifications
- Stacked notifications

### ReportExportUtil
Export functionality:
- CSV export for all tables
- Student report cards
- Class performance reports
- Attendance reports

### SearchUtil
Advanced search capabilities:
- Multi-criteria filtering
- Real-time search
- Memory filtering
- Filter presets

### Session
Global session management:
- User session data
- Faculty session data
- Thread-safe singleton

---

## Styling

### CSS Framework
Modern CSS with:
- CSS variables for theming
- Light/Dark theme support
- Responsive design
- Professional components

### Components
- Cards with shadows
- Navigation buttons
- Form controls
- Tables with hover effects
- Notification styles

---

## Features

### Admin Features
1. Dashboard with statistics
2. Student management (CRUD)
3. Faculty management
4. Subject management
5. Subject assignment to faculty
6. User creation
7. Database backup

### Faculty Features
1. Dashboard with statistics
2. Student attendance marking
3. Marks entry
4. Grade assignment
5. Attendance viewing
6. Marksheet generation

---

## Build & Run

### Prerequisites
- Java 17+
- JavaFX SDK 21
- MySQL 8.0+ or SQLite

### IDE Setup
1. Import project
2. Add JavaFX libraries
3. Add MySQL Connector (if using MySQL)
4. Run Main.java

### Command Line
```bash
javac -d bin -cp "lib/*" src/controller/Main.java
java -cp "bin:lib/*" controller.Main
```

---

## Configuration

### Environment Variables
- `DB_TYPE`: Database type (MYSQL/SQLITE)
- `DB_URL`: JDBC URL
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password
- `DB_SQLITE`: SQLite file path

### Default Credentials
- **Admin**: username=admin, password=admin123

---

## File Structure

```
student_management_project/
├── src/
│   ├── controller/    # UI Controllers
│   ├── model/         # Domain Models
│   ├── dao/           # Data Access Objects
│   └── util/          # Utilities
├── resources/
│   ├── fxml/          # UI Layouts
│   ├── css/           # Stylesheets
│   └── images/        # Assets
├── database/
│   └── schema.sql     # Database Schema
├── screenshots/       # UI Screenshots
├── docs/             # Documentation
├── .gitignore
└── README.md
```

---

## Performance Considerations

1. **Database**: Use connection pooling
2. **UI**: Load data in background threads
3. **Search**: Implement pagination for large datasets
4. **Images**: Optimize image assets
5. **Memory**: Use weak references where possible

---

## Testing

### Unit Tests
- DAO layer testing
- Validation testing
- Utility method testing

### Integration Tests
- Database operations
- UI workflows
- Authentication flows

---

## Deployment

### Packaging
1. Create JAR with dependencies
2. Include resources
3. Set up database
4. Configure environment

### Distribution
- Standalone JAR
- Installer package
- Docker container

---

## Maintenance

### Regular Tasks
- Database backups
- Log rotation
- Performance monitoring
- Security updates

### Troubleshooting
- Check database connection
- Verify JavaFX installation
- Review error logs
- Validate configuration

---

## Future Enhancements

1. REST API for mobile apps
2. Email notifications
3. Advanced analytics dashboard
4. Student self-service portal
5. Multi-language support
6. Dark/Light theme toggle
7. Database migration tools
8. Automated testing suite

---

## License

MIT License - See LICENSE file for details

---

## Support

For issues and feature requests, please use GitHub Issues.

For contributions, please submit a Pull Request.
