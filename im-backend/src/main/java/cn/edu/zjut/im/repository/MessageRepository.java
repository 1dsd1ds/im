package cn.edu.zjut.im.repository;

import cn.edu.zjut.im.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 拉取发给某用户的离线消息（未读）
    List<Message> findByToUserIdAndStatusOrderByCreatedAtAsc(Long toUserId, String status);

    // 获取与某联系人的历史消息（双向）
    @Query("SELECT m FROM Message m WHERE m.groupId IS NULL AND " +
            "((m.fromUserId = :userId1 AND m.toUserId = :userId2) OR " +
            "(m.fromUserId = :userId2 AND m.toUserId = :userId1)) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findChatHistory(@Param("userId1") Long userId1,
                                  @Param("userId2") Long userId2,
                                  Pageable pageable);

    // 获取群聊消息
    Page<Message> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    // 获取每个会话的最后一条消息
    @Query(value = "SELECT m.* FROM im_messages m WHERE m.id IN (" +
            "SELECT MAX(m2.id) FROM im_messages m2 WHERE m2.group_id IS NULL AND " +
            "(m2.from_user_id = :userId OR m2.to_user_id = :userId) " +
            "GROUP BY CASE WHEN m2.from_user_id = :userId THEN m2.to_user_id ELSE m2.from_user_id END" +
            ") ORDER BY m.created_at DESC",
            nativeQuery = true)
    List<Message> findLastMessagesForUser(@Param("userId") Long userId);

    // 计算某用户对某联系人的未读消息数
    @Query("SELECT COUNT(m) FROM Message m WHERE m.fromUserId = :fromUserId " +
            "AND m.toUserId = :toUserId AND m.status = 'UNREAD' AND m.groupId IS NULL")
    int countUnread(@Param("fromUserId") Long fromUserId,
                    @Param("toUserId") Long toUserId);

    // 批量标记已读
    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ' WHERE m.fromUserId = :fromUserId " +
            "AND m.toUserId = :toUserId AND m.status = 'UNREAD' AND m.groupId IS NULL")
    void markAsRead(@Param("fromUserId") Long fromUserId,
                    @Param("toUserId") Long toUserId);

    // 总未读消息数
    @Query("SELECT COUNT(m) FROM Message m WHERE m.toUserId = :userId AND m.status = 'UNREAD'")
    int countTotalUnread(@Param("userId") Long userId);
}
