package dev.camilo.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import lombok.Data;

import java.util.List;

@Entity
@Data
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private String accountNumber;
  private String names;
  private String surename;
  private String phone;
  private String address;
  @OneToMany(
      mappedBy = "customer",
      cascade = {CascadeType.ALL},
      fetch = FetchType.EAGER)
  @JsonManagedReference
  private List<Product> products;
}