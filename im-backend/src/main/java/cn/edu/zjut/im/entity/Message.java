package cn.edu.zjut.im.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "msg_type", length = 10)
    private String msgType = "TEXT";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 10)
    private String status = "UNREAD";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
