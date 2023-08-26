package dev.camilo.entities.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import lombok.Data;

import java.util.List;

@Entity
@Data
public class Customer extends PanacheEntity {

  private String accountNumber;
  private String names;
  private String surname;
  private String phone;
  private String address;
  @OneToMany(
      mappedBy = "customer",
      cascade = {CascadeType.ALL},
      fetch = FetchType.EAGER)
  @JsonManagedReference
  private List<Product> products;
}