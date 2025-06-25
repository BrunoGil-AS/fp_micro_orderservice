package com.aspiresys.fp_micro_orderservice.user;

import java.util.List;

import com.aspiresys.fp_micro_orderservice.order.Order;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a user entity in the system.
 * <p>
 * This class is mapped to the "users" table in the database and contains
 * basic user information such as first name, last name, and email.
 * </p>
 *
 * <p>
 * Annotations:
 * <ul>
 *   <li>{@code @Entity} - Specifies that the class is an entity.</li>
 *   <li>{@code @Table(name = "users")} - Maps the entity to the "users" table.</li>
 *   <li>{@code @Getter}, {@code @Setter} - Lombok annotations to generate getters and setters.</li>
 *   <li>{@code @NoArgsConstructor}, {@code @AllArgsConstructor}, {@code @Builder} - Lombok annotations for constructors and builder pattern.</li>
 * </ul>
 * </p>
 *
 * Fields:
 * <ul>
 *   <li>{@code id} - The unique identifier for the user (primary key).</li>
 *   <li>{@code firstName} - The user's first name.</li>
 *   <li>{@code lastName} - The user's last name.</li>
 *   <li>{@code email} - The user's email address.</li>
 * </ul>
 * 
 * <p> * @author bruno.gil </p>
 * <p>See {@link Order} for related entities.</p>
 * 
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class User {
    @Id 
    private Long id;

    private String firstName;
    private String lastName;
    private String email;

    @OneToMany(mappedBy = "user")
    private List<Order> orders;

}
