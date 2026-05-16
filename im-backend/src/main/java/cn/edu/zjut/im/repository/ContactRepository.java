package cn.edu.zjut.im.repository;

import cn.edu.zjut.im.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserIdAndStatus(Long userId, String status);

    List<Contact> findByContactIdAndStatus(Long contactId, String status);

    boolean existsByUserIdAndContactId(Long userId, Long contactId);

    @Query("SELECT c FROM Contact c WHERE c.userId = :userId1 AND c.contactId = :userId2 " +
            "OR c.userId = :userId2 AND c.contactId = :userId1")
    List<Contact> findBidirectional(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
