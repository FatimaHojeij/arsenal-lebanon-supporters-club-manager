package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByGameIdAndStatus(Long gameId, ApplicationStatus status);
    List<Application> findByMember(Member member);
    List<Application> findByMemberAndStatus(Member member, ApplicationStatus status);
    boolean existsByMemberAndGame(Member member, Game game);
}
