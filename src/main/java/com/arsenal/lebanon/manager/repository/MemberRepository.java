package com.arsenal.lebanon.manager.repository;

import com.arsenal.lebanon.manager.model.Member;
import com.arsenal.lebanon.manager.model.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByALSCMembershipNumber(long ALSCMembershipNumber);
    List<Member> findByStatus(MembershipStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.status = com.arsenal.lebanon.manager.model.MembershipStatus.Lapsed " +
            "WHERE m.expiryDate < CURRENT_DATE " +
            "AND m.status != com.arsenal.lebanon.manager.model.MembershipStatus.Banned " +
            "AND m.memberType NOT IN (" +
            "  com.arsenal.lebanon.manager.model.MemberType.President, " +
            "  com.arsenal.lebanon.manager.model.MemberType.Secretary, " +
            "  com.arsenal.lebanon.manager.model.MemberType.Treasurer, " +
            "  com.arsenal.lebanon.manager.model.MemberType.Permanent" +
            ")")
    int updateExpiredMemberships();


}
