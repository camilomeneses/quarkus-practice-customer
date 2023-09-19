package dev.camilo.graphql;

import dev.camilo.entities.jpa.Customer;
import dev.camilo.service.CustomerService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;

import java.util.List;

@GraphQLApi
public class CustomerResource {

  @Inject
  CustomerService customerService;

  @Query("allCustomers")
  @Description("Obtener todos los clientes desde la base de datos")
  public Uni<List<Customer>> getAllCustomers(){
    return customerService.findAll();
  }


  @Query
  @Description("Obtener un customer desde la base de datos")
  public Uni<Customer> getCustomer(@Name("id") Long id){
    return customerService.getById(id);
  }

  @Mutation
  public Uni<Customer> addCustomer (Customer customer){
    return  customerService.addMutation(customer);
  }

  @Mutation
  public Uni<Boolean> deleteCustomer(Long id){
    return customerService.deleteMutation(id);
  }
}
