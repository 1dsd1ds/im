package cn.edu.zjut.im.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_contacts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "contact_id"})
})
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "contact_id", nullable = false)
    private Long contactId;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(length = 50)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
