package dev.camilo.controllers;


import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import dev.camilo.service.CustomerService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerApi {

  @Inject
  CustomerService customerService;

  @GET
  public Uni<List<Customer>> list() {
    return customerService.list();
  }

  @GET
  @Path("/using-repository")
  public Uni<List<Customer>> listUsingRepository() {
    return customerService.findAll();
  }

  @GET
  @Path("/{id}")
  public Uni<Customer> getById(@PathParam("id") Long id) {
    return customerService.getById(id);
  }

  // llamada de valores de producto de manera reactiva
  @GET
  @Path("/{id}/product")
  public Uni<Customer> getCustomerProductsById(@PathParam("id") Long id) {
    return customerService.getCustomerProductsById(id);
  }

  @POST
  public Uni<Response> add(Customer c) {
    return customerService.add(c);
  }

  @DELETE
  @Path("/{id}")
  public Uni<Response> delete(@PathParam("id") Long id) {
    return customerService.delete(id);
  }

  @PUT
  @Path("/{id}")
  public Uni<Response> update(@PathParam("id") String id, Customer customer) {
    return customerService.update(id, customer);
  }

  @GET
  @Path("/products-graphql")
  @Blocking
  public List<Product> getAllProductsGraphQl() throws Exception {
    return customerService.getProductsGraphQL();
  }

  @GET
  @Path("/{id}/product-graphql")
  @Blocking
  public Product getProductGraphQl(@PathParam("id") Long id) throws Exception {
    return customerService.getByIdProductGraphQL(id);
  }

}