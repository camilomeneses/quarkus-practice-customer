package dev.camilo.entities.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
    uniqueConstraints=
    @UniqueConstraint(columnNames={"customer", "product"})
)
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  @ManyToOne
  @JoinColumn(name = "customer", referencedColumnName = "id")
  @JsonBackReference
  private Customer customer;
  @Column
  private Long product;
  @Transient
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;
  @Transient
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String description;
}
