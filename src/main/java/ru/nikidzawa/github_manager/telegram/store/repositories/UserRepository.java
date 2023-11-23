package ru.nikidzawa.github_manager.telegram.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nikidzawa.github_manager.telegram.store.entities.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
