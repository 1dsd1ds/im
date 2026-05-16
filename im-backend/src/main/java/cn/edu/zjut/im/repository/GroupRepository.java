package cn.edu.zjut.im.repository;

import cn.edu.zjut.im.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByOwnerId(Long ownerId);
}
