package com.ticket.partner.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends BaseEntity {
    private Long id;
    private Long operatorId;
    private String fullName;
    private String phone;
    private String licenseNumber;
    private String licenseClass;
    private DriverStatus status;

    public void validate() {
        if (operatorId == null) {
            throw new BusinessException("INVALID_DRIVER", "Operator ID is required");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new BusinessException("INVALID_DRIVER_NAME", "Driver full name cannot be blank");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("INVALID_DRIVER_PHONE", "Driver phone cannot be blank");
        }
        if (licenseNumber == null || licenseNumber.trim().isEmpty()) {
            throw new BusinessException("INVALID_DRIVER_LICENSE", "Driver license number cannot be blank");
        }
    }
}
