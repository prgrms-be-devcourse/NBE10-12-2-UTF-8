package com.back.domain.match.matchRequest.repository;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Integer> {

}
