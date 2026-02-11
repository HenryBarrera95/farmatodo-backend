package com.farmatodo.token;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTokenRepository extends JpaRepository<CardToken, String> {
}
