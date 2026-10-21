package com.littlehorizons.erp.model;

/** Result of generating the monthly charges: how many were created and how many already existed. */
public record ChargeGenerationResult(int created, int alreadyExisted) {
}
