package dev.camilo.repositories;

import dev.camilo.entities.jpa.Customer;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerRepository implements PanacheRepositoryBase<Customer,Long> {
}