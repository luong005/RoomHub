package com.roommate_connection_platform.securitylog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
}
