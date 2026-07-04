package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Application;
import com.arsenal.lebanon.manager.model.ApplicationStatus;
import com.arsenal.lebanon.manager.model.Game;
import com.arsenal.lebanon.manager.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByGameId(Long gameId);
    List<Application> findByGameIdAndStatus(Long gameId, ApplicationStatus status);
    List<Application> findByMember(Member member);
    List<Application> findByMemberAndStatus(Member member, ApplicationStatus status);
    boolean existsByMemberAndGame(Member member, Game game);
    @Query("SELECT a FROM Application a WHERE a.member = :member " +
            "AND a.status = com.arsenal.lebanon.manager.model.ApplicationStatus.Pending " +
            "AND a.game.applicationsOpen = true " +
            "AND a.game.matchDate > CURRENT_DATE")
    List<Application> findRescorableApplications(@Param("member") Member member);
}
