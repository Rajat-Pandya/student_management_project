# Developer Setup Guide

## Prerequisites

### Required Software
1. **Java Development Kit (JDK) 17+**
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/
   - Verify installation: `java -version`

2. **JavaFX SDK 21**
   - Download from: https://gluonhq.com/products/javafx/
   - Extract to a known location

3. **MySQL 8.0+** (Optional - SQLite works out of the box)
   - Download from: https://dev.mysql.com/downloads/mysql/
   - Install MySQL Server and Workbench

4. **IDE (Choose one)**
   - Eclipse IDE for Java Developers: https://www.eclipse.org/
   - IntelliJ IDEA Community: https://www.jetbrains.com/idea/
   - VS Code with Java Extension Pack

### Required Libraries

Download these JAR files and place them in a `lib/` folder:

1. **MySQL Connector/J** (if using MySQL)
   - https://dev.mysql.com/downloads/connector/j/

2. **jBCrypt** (Password hashing)
   - https://mvnrepository.com/artifact/org.mindrot/jbcrypt

3. **Apache PDFBox** (PDF export)
   - https://pdfbox.apache.org/download.html

---

## Project Setup

### Option 1: Eclipse IDE

1. **Import Project**
   ```
   File → Import → Existing Projects into Workspace
   Select the project folder
   ```

2. **Add JavaFX Libraries**
   ```
   Right-click Project → Build Path → Configure Build Path
   Libraries → Add External JARs
   Select all JavaFX JARs from JavaFX SDK
   ```

3. **Add MySQL Connector** (if using MySQL)
   ```
   Build Path → Add External JARs
   Select mysql-connector-java-8.x.xx.jar
   ```

4. **Add Other Dependencies**
   ```
   Build Path → Add External JARs
   Add jbcrypt.jar and pdfbox.jar
   ```

5. **Run Configuration**
   ```
   Right-click Main.java → Run As → Run Configurations
   Arguments → VM Arguments:
   --module-path "path/to/javafx/lib" --add-modules javafx.controls,javafx.fxml
   ```

### Option 2: IntelliJ IDEA

1. **Open Project**
   ```
   File → Open → Select project folder
   ```

2. **Project Structure**
   ```
   File → Project Structure → Libraries
   Add JavaFX SDK lib folder
   Add MySQL connector JAR
   Add jbcrypt JAR
   Add pdfbox JAR
   ```

3. **Run Configuration**
   ```
   Run → Edit Configurations
   Add VM Options:
   --module-path "path/to/javafx/lib" --add-modules javafx.controls,javafx.fxml
   ```

### Option 3: VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - JavaFX Support

2. **Configure Settings**
   ```json
   {
     "java.project.referencedLibraries": [
       "lib/**/*.jar",
       "path/to/javafx/lib/*.jar"
     ],
     "java.jdt.ls.vmargs": "--module-path path/to/javafx/lib --add-modules javafx.controls,javafx.fxml"
   }
   ```

3. **Launch Configuration**
   ```json
   {
     "type": "java",
     "name": "Launch Main",
     "request": "launch",
     "mainClass": "controller.Main",
     "vmArgs": "--module-path path/to/javafx/lib --add-modules javafx.controls,javafx.fxml"
   }
   ```

---

## Database Setup

### Option 1: SQLite (Recommended for Development)

No setup required! The application will automatically create `student_db.db` file.

### Option 2: MySQL

1. **Create Database**
   ```sql
   CREATE DATABASE student_db;
   USE student_db;
   ```

2. **Run Schema Script**
   ```bash
   mysql -u root -p student_db < database/schema.sql
   ```

3. **Configure Connection**
   Set environment variables or update `DatabaseConnection.java`:
   ```java
   private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/student_db";
   private static final String MYSQL_USER = "root";
   private static final String MYSQL_PASSWORD = "your_password";
   ```

---

## Running the Application

### From IDE
1. Navigate to `src/controller/Main.java`
2. Right-click and select Run
3. The login screen should appear

### From Command Line

1. **Compile**
   ```bash
   javac -d bin -cp "lib/*:resources" src/**/*.java
   ```

2. **Run**
   ```bash
   java -cp "bin:lib/*:resources" \
        --module-path path/to/javafx/lib \
        --add-modules javafx.controls,javafx.fxml \
        controller.Main
   ```

---

## Default Login

After running the application:

- **Username**: `admin`
- **Password**: `admin123`

---

## Development Workflow

### 1. Making Changes

1. Create a new branch
   ```bash
   git checkout -b feature/my-feature
   ```

2. Make your changes
3. Test thoroughly
4. Commit with descriptive messages

### 2. Testing

1. **Unit Tests**
   - Test DAO classes
   - Test utility methods
   - Test validation logic

2. **Integration Tests**
   - Test UI workflows
   - Test database operations
   - Test authentication

3. **Manual Testing**
   - Test all user flows
   - Verify UI responsiveness
   - Check error handling

### 3. Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods focused and short

---

## Troubleshooting

### Common Issues

1. **JavaFX Not Found**
   - Verify JavaFX SDK path in VM arguments
   - Ensure all required modules are added

2. **Database Connection Failed**
   - Check MySQL service is running
   - Verify credentials
   - Check database exists

3. **Class Not Found Errors**
   - Ensure all JARs are in classpath
   - Verify build path configuration

4. **FXML Load Errors**
   - Check file paths in code
   - Verify controller class names

### Debug Tips

1. Enable debug logging:
   ```java
   System.setProperty("javafx.verbose", "true");
   ```

2. Check console output for stack traces
3. Use IDE debugger with breakpoints
4. Review log files

---

## Useful Resources

- **JavaFX Documentation**: https://openjfx.io/
- **JavaFX CSS Reference**: https://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
- **MySQL Documentation**: https://dev.mysql.com/doc/
- **jBCrypt Documentation**: https://github.com/jeremyh/jBCrypt

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

See README.md for more details.
