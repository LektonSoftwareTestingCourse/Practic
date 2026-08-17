package com.processing.merchantacquirer.repository;

import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.merchantacquirer.domain.repository.TerminalRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, String>, TerminalRepositoryPort {
}
