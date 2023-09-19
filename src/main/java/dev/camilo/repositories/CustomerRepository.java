package dev.camilo.repositories;

import dev.camilo.entities.jpa.Customer;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

import org.springframework.stereotype.Repository;


@Repository
public class CustomerRepository implements PanacheRepositoryBase<Customer,Long> {
}