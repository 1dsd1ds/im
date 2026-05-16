package cn.edu.zjut.im.repository;

import cn.edu.zjut.im.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByNicknameContainingOrUsernameContaining(String nickname, String username);
}
