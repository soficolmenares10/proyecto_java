package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for students. It validates the data and the rules,
 * and delegates the saving to the DAOs.
 */
public class StudentController {

    private static final int MIN_AGE = 2;
    private static final int MAX_AGE = 25;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public StudentController(StudentDao studentDao, GuardianDao guardianDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    /**
     * Registers a new student with its first guardian (required by the school rules).
     *
     * @return the new student id
     */
    public int register(Student student, int primaryGuardianId) {
        validate(student, 0);
        if (guardianDao.findById(primaryGuardianId).isEmpty()) {
            throw new BusinessException("El acudiente seleccionado no existe.");
        }
        student.setStatus("Activo");
        return studentDao.insert(student, primaryGuardianId);
    }

    public void update(Student student) {
        if (studentDao.findById(student.getId()).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        validate(student, student.getId());
        studentDao.update(student);
    }

    /**
     * Only "Activo" and "Suspendido" can be set here. "Retirado" and "Graduado"
     * are set through the enrollment (withdraw / graduate) so both tables stay consistent.
     */
    public void changeStatus(int studentId, String newStatus) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no existe."));
        Rules.requireOneOf(newStatus, List.of("Activo", "Suspendido"), "El estado");
        if ("Graduado".equals(student.getStatus())) {
            throw new BusinessException("Un estudiante graduado no puede cambiar de estado.");
        }
        if (newStatus.equals(student.getStatus())) {
            throw new BusinessException("El estudiante ya está en estado " + newStatus + ".");
        }
        studentDao.updateStatus(studentId, newStatus);
    }

    public Student getByDocument(String documentNumber) {
        return studentDao.findByDocument(documentNumber)
                .orElseThrow(() -> new BusinessException(
                        "No existe un estudiante con el documento " + documentNumber + "."));
    }

    public boolean isDocumentTaken(String documentNumber) {
        return studentDao.existsByDocument(documentNumber, 0);
    }

    public List<Student> listAll() {
        return studentDao.findAll();
    }

    public List<Student> search(String text) {
        if (!Validator.isNotBlank(text)) {
            throw new BusinessException("Escriba un texto para buscar.");
        }
        return studentDao.search(text.trim());
    }

    /** How many students there are in each status (ordered by status name). */
    public Map<String, Long> countByStatus() {
        return studentDao.findAll().stream()
                .collect(Collectors.groupingBy(Student::getStatus, TreeMap::new, Collectors.counting()));
    }

    private void validate(Student student, int excludeId) {
        Rules.requireText(student.getFirstName(), "El primer nombre", 100);
        Rules.limitText(student.getMiddleName(), "El segundo nombre", 100);
        Rules.requireText(student.getLastName(), "El primer apellido", 100);
        Rules.limitText(student.getSecondLastName(), "El segundo apellido", 100);
        Rules.requireOneOf(student.getDocumentType(), Student.DOCUMENT_TYPES, "El tipo de documento");
        if (!Validator.isValidDocument(student.getDocumentNumber())) {
            throw new BusinessException("El número de documento debe tener de 5 a 20 letras o números.");
        }
        if (studentDao.existsByDocument(student.getDocumentNumber(), excludeId)) {
            throw new BusinessException("Ya existe un estudiante con el documento "
                    + student.getDocumentNumber() + ".");
        }

        LocalDate today = LocalDate.now();
        if (student.getBirthDate() == null || !student.getBirthDate().isBefore(today)) {
            throw new BusinessException("La fecha de nacimiento debe ser anterior a hoy.");
        }
        int age = student.getAge();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BusinessException("La edad del estudiante (" + age + " años) debe estar entre "
                    + MIN_AGE + " y " + MAX_AGE + " años.");
        }
        if (student.getAdmissionDate() == null
                || student.getAdmissionDate().isBefore(student.getBirthDate())
                || student.getAdmissionDate().isAfter(today)) {
            throw new BusinessException("La fecha de ingreso no puede ser anterior al nacimiento ni posterior a hoy.");
        }

        Rules.requireOneOf(student.getGender(), Student.GENDERS, "El género");
        Rules.limitText(student.getAddress(), "La dirección", 255);
        Rules.optionalPhone(student.getPhone(), "El teléfono");
        Rules.optionalEmail(student.getEmail(), "El correo");
        Rules.limitText(student.getEmail(), "El correo", 100);
        if (student.getBloodType() != null) {
            Rules.requireOneOf(student.getBloodType(), Student.BLOOD_TYPES, "El tipo de sangre");
        }
    }
}
