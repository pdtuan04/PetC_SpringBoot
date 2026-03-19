package com.hutech.coca.repository;

import com.hutech.coca.model.Invitation;
import com.hutech.coca.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IInvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByInviterUsernameAndStatus(String inviterUsername, InvitationStatus status);

    Optional<Invitation> findByInviteeEmailAndStatus(String inviteeEmail, InvitationStatus status);
}
