package com.littlehorizons.erp.model;

/** Extracurricular participation of one student: active clubs, events and podium places. */
public record ParticipationSummary(int studentId, String studentName, String studentDocument, long activeClubs,
                                   long events, long podiums) {

    public long totalActivities() {
        return activeClubs + events;
    }
}
