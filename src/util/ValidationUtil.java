package util;

import java.util.regex.Pattern;

/**
 * Validation utility class for the Student Management System.
 * Provides comprehensive validation for forms, inputs, and data.
 */
public class ValidationUtil {
    
    // Regex patterns
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^[+]?[0-9]{10,15}$");
    
    private static final Pattern ROLL_NUMBER_PATTERN = 
        Pattern.compile("^[A-Za-z0-9/-]+$");
    
    private static final Pattern NAME_PATTERN = 
        Pattern.compile("^[A-Za-z\\s'-]+$");
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[A-Za-z0-9_-]{3,50}$");
    
	private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$");
    
    // Validation result class
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    // ==================== String Validations ====================
    
    /**
     * Validate that a string is not null or empty.
     */
    public static ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " is required.");
        }
        return ValidationResult.success();
    }
    
    /**
     * Validate minimum length.
     */
    public static ValidationResult validateMinLength(String value, int minLength, String fieldName) {
        if (value == null || value.length() < minLength) {
            return ValidationResult.failure(fieldName + " must be at least " + minLength + " characters.");
        }
        return ValidationResult.success();
    }
    
    /**
     * Validate maximum length.
     */
    public static ValidationResult validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            return ValidationResult.failure(fieldName + " must not exceed " + maxLength + " characters.");
        }
        return ValidationResult.success();
    }
    
    /**
     * Validate length range.
     */
    public static ValidationResult validateLength(String value, int minLength, int maxLength, String fieldName) {
        ValidationResult required = validateRequired(value, fieldName);
        if (!required.isValid()) return required;
        
        ValidationResult min = validateMinLength(value, minLength, fieldName);
        if (!min.isValid()) return min;
        
        ValidationResult max = validateMaxLength(value, maxLength, fieldName);
        if (!max.isValid()) return max;
        
        return ValidationResult.success();
    }
    
    // ==================== Email Validation ====================
    
    /**
     * Validate email format.
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("Email is required.");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.failure("Invalid email format. Please enter a valid email address.");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate optional email (if provided).
     */
    public static ValidationResult validateOptionalEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.success();
        }
        return validateEmail(email);
    }
    
    // ==================== Phone Validation ====================
    
    /**
     * Validate phone number.
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.success(); // Phone is optional
        }
        
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            return ValidationResult.failure("Invalid phone number. Please enter 10-15 digits.");
        }
        
        return ValidationResult.success();
    }
    
    // ==================== Name Validation ====================
    
    /**
     * Validate person name.
     */
    public static ValidationResult validateName(String name, String fieldName) {
        ValidationResult required = validateRequired(name, fieldName);
        if (!required.isValid()) return required;
        
        if (!NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.failure(fieldName + " can only contain letters, spaces, hyphens, and apostrophes.");
        }
        
        ValidationResult length = validateLength(name, 2, 100, fieldName);
        if (!length.isValid()) return length;
        
        return ValidationResult.success();
    }
    
    // ==================== Roll Number Validation ====================
    
    /**
     * Validate roll number.
     */
    public static ValidationResult validateRollNumber(String rollNumber) {
        ValidationResult required = validateRequired(rollNumber, "Roll number");
        if (!required.isValid()) return required;
        
        if (!ROLL_NUMBER_PATTERN.matcher(rollNumber).matches()) {
            return ValidationResult.failure("Roll number can only contain letters, numbers, hyphens, and slashes.");
        }
        
        ValidationResult length = validateLength(rollNumber, 3, 30, "Roll number");
        if (!length.isValid()) return length;
        
        return ValidationResult.success();
    }
    
    // ==================== Username Validation ====================
    
    /**
     * Validate username.
     */
    public static ValidationResult validateUsername(String username) {
        ValidationResult required = validateRequired(username, "Username");
        if (!required.isValid()) return required;
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.failure("Username must be 3-50 characters and can only contain letters, numbers, underscores, and hyphens.");
        }
        
        return ValidationResult.success();
    }
    
    // ==================== Password Validation ====================
    
    /**
     * Validate password strength.
     */
    public static ValidationResult validatePassword(String password) {
        ValidationResult required = validateRequired(password, "Password");
        if (!required.isValid()) return required;
        
        ValidationResult minLength = validateMinLength(password, 8, "Password");
        if (!minLength.isValid()) return minLength;
        
        // Check for complexity (at least one digit, one lowercase, one uppercase, one special char)
        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if ("!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0) hasSpecial = true;
        }
        
        StringBuilder missing = new StringBuilder();
        if (!hasDigit) missing.append("digit, ");
        if (!hasLower) missing.append("lowercase letter, ");
        if (!hasUpper) missing.append("uppercase letter, ");
        if (!hasSpecial) missing.append("special character, ");
        
        if (missing.length() > 0) {
            String msg = missing.substring(0, missing.length() - 2);
            return ValidationResult.failure("Password must contain at least one " + msg + ".");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate password confirmation.
     */
    public static ValidationResult validatePasswordMatch(String password, String confirmPassword) {
        ValidationResult required = validateRequired(confirmPassword, "Confirm password");
        if (!required.isValid()) return required;
        
        if (!password.equals(confirmPassword)) {
            return ValidationResult.failure("Passwords do not match.");
        }
        
        return ValidationResult.success();
    }
    
    // ==================== Numeric Validations ====================
    
    /**
     * Validate positive integer.
     */
    public static ValidationResult validatePositiveInteger(String value, String fieldName) {
        ValidationResult required = validateRequired(value, fieldName);
        if (!required.isValid()) return required;
        
        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue <= 0) {
                return ValidationResult.failure(fieldName + " must be a positive number.");
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure(fieldName + " must be a valid number.");
        }
    }
    
    /**
     * Validate marks (0-100).
     */
    public static ValidationResult validateMarks(String value) {
        ValidationResult required = validateRequired(value, "Marks");
        if (!required.isValid()) return required;
        
        try {
            double marks = Double.parseDouble(value.trim());
            if (marks < 0 || marks > 100) {
                return ValidationResult.failure("Marks must be between 0 and 100.");
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Marks must be a valid number.");
        }
    }
    
    /**
     * Validate semester (1-8).
     */
    public static ValidationResult validateSemester(String semester) {
        ValidationResult required = validateRequired(semester, "Semester");
        if (!required.isValid()) return required;
        
        try {
            int sem = Integer.parseInt(semester.trim());
            if (sem < 1 || sem > 8) {
                return ValidationResult.failure("Semester must be between 1 and 8.");
            }
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Semester must be a valid number.");
        }
    }
    
    // ==================== Combined Validations ====================
    
    /**
     * Validate multiple fields and return first error.
     */
    public static ValidationResult validateAll(ValidationResult... validations) {
        for (ValidationResult validation : validations) {
            if (!validation.isValid()) {
                return validation;
            }
        }
        return ValidationResult.success();
    }
    
    /**
     * Check if all validations pass.
     */
    public static boolean allValid(ValidationResult... validations) {
        for (ValidationResult validation : validations) {
            if (!validation.isValid()) {
                return false;
            }
        }
        return true;
    }
    
    // ==================== Quick Checks ====================
    
    /**
     * Quick check if string is not empty.
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Quick check if string is a valid number.
     */
    public static boolean isNumeric(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Quick check if string is a valid integer.
     */
    public static boolean isInteger(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Sanitize string input (remove extra spaces, trim).
     */
    public static String sanitize(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Truncate string to maximum length.
     */
    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
