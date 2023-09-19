package dev.camilo.service;

import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface  CustomerService {

  //Reactive REST Methods
  Uni<List<Customer>> list();
  Uni<List<Customer>> findAll();
  Uni<Customer> getById(Long id);
  Uni<Response> add(Customer c);
  Uni<Response> delete(Long id);
  Uni<Response> update(String id, Customer c);
  Uni<Customer> getCustomerProductsById(Long id);

  //GraphQL Methods
  Uni<Customer> addMutation(Customer c);
  Uni<Boolean> deleteMutation(Long id);

  //Client GraphQL Methods
  List<Product> getProductsGraphQL() throws Exception;
  Product getByIdProductGraphQL(Long id) throws Exception;
}

