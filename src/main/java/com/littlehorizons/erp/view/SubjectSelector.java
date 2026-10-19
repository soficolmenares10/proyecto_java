package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reusable helper (not a screen): asks the user for a grade and then for one of its
 * subjects, and returns the chosen subject. Several screens need to pick a subject.
 */
public class SubjectSelector {

    private final SubjectController subjectController;
    private final EnrollmentController enrollmentController;

    public SubjectSelector(SubjectController subjectController, EnrollmentController enrollmentController) {
        this.subjectController = subjectController;
        this.enrollmentController = enrollmentController;
    }

    /** Asks only for a grade. */
    public Grade chooseGrade() {
        List<Grade> grades = enrollmentController.getGrades();
        List<String> names = grades.stream().map(Grade::getName).collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Grado", names);
        return grades.get(names.indexOf(chosen));
    }

    /**
     * Asks for a grade and then for a subject of that grade.
     *
     * @param activeOnly if true, inactive subjects are not offered
     */
    public Subject choose(boolean activeOnly) {
        Grade grade = chooseGrade();
        List<Subject> subjects = activeOnly
                ? subjectController.listActiveByGrade(grade.getId())
                : subjectController.listByGrade(grade.getId());
        if (subjects.isEmpty()) {
            throw new BusinessException("No hay materias disponibles en " + grade.getName() + ".");
        }
        List<String> labels = subjects.stream()
                .map(subject -> subject.getCode() + " - " + subject.getName()
                        + (subject.isActive() ? "" : " (inactiva)"))
                .collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Materia", labels);
        return subjects.get(labels.indexOf(chosen));
    }
}
