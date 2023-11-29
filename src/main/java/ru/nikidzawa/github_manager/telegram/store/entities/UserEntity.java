package ru.nikidzawa.github_manager.telegram.store.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "User_entity")
public class UserEntity {
    @Id
    Long id;
    byte[] token;
}
