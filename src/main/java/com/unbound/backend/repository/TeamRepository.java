package com.unbound.backend.repository;

import com.unbound.backend.entity.Team;
import com.unbound.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Integer> {
    List<Team> findByEvent(Event event);
} 