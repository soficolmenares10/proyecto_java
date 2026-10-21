package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.model.LoanReturn;
import com.littlehorizons.erp.model.Student;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for loans of resources: lending, returning and finding overdue loans.
 */
public class LoanController {

    private static final int MAX_LOAN_DAYS = 30;
    private static final int MAX_OPEN_LOANS_STUDENT = 3;
    private static final int MAX_OPEN_LOANS_PROFESSOR = 5;

    private final LoanDao loanDao;
    private final ResourceDao resourceDao;
    private final StudentDao studentDao;
    private final ProfessorDao professorDao;

    public LoanController(LoanDao loanDao, ResourceDao resourceDao, StudentDao studentDao,
                          ProfessorDao professorDao) {
        this.loanDao = loanDao;
        this.resourceDao = resourceDao;
        this.studentDao = studentDao;
        this.professorDao = professorDao;
    }

    /**
     * Lends a resource for a number of days (1 to 30). The borrower must be active, the resource
     * must be available, and students can have at most 3 open loans (professors, 5).
     *
     * @return the new loan id
     */
    public int lend(int resourceId, BorrowerType type, int borrowerId, int days, String notes) {
        Resource resource = resourceDao.findById(resourceId)
                .orElseThrow(() -> new BusinessException("El recurso no existe."));
        if (!Resource.AVAILABLE.equals(resource.getStatus())) {
            throw new BusinessException("El recurso " + resource.getName() + " no está disponible. Estado actual: "
                    + resource.getStatus() + ".");
        }
        checkBorrower(type, borrowerId);
        if (days < 1 || days > MAX_LOAN_DAYS) {
            throw new BusinessException("Los días de préstamo deben estar entre 1 y " + MAX_LOAN_DAYS + ".");
        }
        Rules.limitText(notes, "Las observaciones", 1000);
        int limit = type == BorrowerType.STUDENT ? MAX_OPEN_LOANS_STUDENT : MAX_OPEN_LOANS_PROFESSOR;
        if (loanDao.countOpenByBorrower(type, borrowerId) >= limit) {
            throw new BusinessException("Ya tiene " + limit + " préstamos abiertos, el máximo permitido.");
        }
        LocalDate today = LocalDate.now();
        int loanId = loanDao.lend(resourceId, type, borrowerId, today, today.plusDays(days), notes);
        if (loanId == -1) {
            throw new BusinessException("El recurso acaba de ser prestado a otra persona.");
        }
        return loanId;
    }

    /** Registers the return of a resource and says how many days late it was (0 if on time). */
    public LoanReturn giveBack(int resourceId) {
        ResourceLoan loan = loanDao.findOpenByResource(resourceId)
                .orElseThrow(() -> new BusinessException("Ese recurso no tiene un préstamo abierto."));
        LocalDate today = LocalDate.now();
        if (!loanDao.giveBack(loan.borrowerType(), loan.id(), today)) {
            throw new BusinessException("El préstamo ya había sido devuelto.");
        }
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.dueDate(), today));
        return new LoanReturn(loan, daysLate);
    }

    /** Marks as "Vencido" the loans whose due date passed, and returns all the overdue loans. */
    public List<ResourceLoan> overdueLoans() {
        loanDao.markOverdue(LocalDate.now());
        return loanDao.findOverdue();
    }

    public List<ResourceLoan> openLoans() {
        loanDao.markOverdue(LocalDate.now());
        return loanDao.findOpen();
    }

    public List<ResourceLoan> historyOfBorrower(BorrowerType type, int borrowerId) {
        return loanDao.findByBorrower(type, borrowerId);
    }

    public List<ResourceLoan> historyOfResource(int resourceId) {
        return loanDao.findByResource(resourceId);
    }

    private void checkBorrower(BorrowerType type, int borrowerId) {
        if (type == BorrowerType.STUDENT) {
            Student student = studentDao.findById(borrowerId)
                    .orElseThrow(() -> new BusinessException("El estudiante no existe."));
            if (!"Activo".equals(student.getStatus())) {
                throw new BusinessException("Solo los estudiantes activos pueden pedir préstamos. Estado actual: "
                        + student.getStatus() + ".");
            }
        } else {
            Professor professor = professorDao.findById(borrowerId)
                    .orElseThrow(() -> new BusinessException("El profesor no existe."));
            if (!"Activo".equals(professor.getStatus())) {
                throw new BusinessException("Solo los profesores activos pueden pedir préstamos. Estado actual: "
                        + professor.getStatus() + ".");
            }
        }
    }
}
