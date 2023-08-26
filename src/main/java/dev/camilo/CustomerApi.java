package dev.camilo;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import dev.camilo.entities.view.CustomerView;
import dev.camilo.repositories.CustomerRepository;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerApi {
  @Inject
  CustomerRepository customerRepository;

  @Inject
  Vertx vertx;

  private WebClient webClient;

  @PostConstruct
  void initialize() {
    this.webClient = WebClient.create(vertx,
        new WebClientOptions().setDefaultHost("localhost")
            .setDefaultPort(9090).setSsl(false).setTrustAll(true));
  }

  @GET
  @Blocking
  public List<CustomerView> list() {
    return customerRepository.listCustomer();
  }

  @GET
  @Path("/{id}")
  @Blocking
  public Customer getById(@PathParam("id") Long id) {
    return customerRepository.findCustomer(id);
  }

  // llamada de valores de producto de manera reactiva
  // usando Uni
  @GET
  @Path("/{id}/product")
  @Blocking
  public Uni<Customer> getCustomerProductsById(@PathParam("id") Long id) {
    /* tenemos dos unis, el uno trae los valores del customer de manera reactiva y
    * el otro trae los productos con el webClient reactivo (todos)*/
    return Uni.combine().all().unis(getCustomerReactive(id), getAllProducts())
        /*combinamos dos unis*/
        .combinedWith((v1, v2) -> {
          v1.getProducts().forEach(product -> {
            v2.forEach(p -> {
              /* recorremos los streams y verificamos los ids de los products del customer
              * y los ids del stream de products*/
              if (product.getId().equals(p.getId())) {
                /* llenamos los valores con los products correspondientes*/
                product.setName(p.getName());
                product.setDescription(p.getDescription());
              }
            });
          });
          /*retornamos el valor del customer con los valores de los products*/
          return v1;
        });
  }

  @POST
  @Blocking
  public Response add(Customer c) {
    c.getProducts().forEach(p -> p.setCustomer(c));
    customerRepository.createdCustomer(c);
    return Response.ok().build();
  }

  @DELETE
  @Path("/{id}")
  @Blocking
  public Response delete(@PathParam("id") Long id) {
    Customer customer = customerRepository.findCustomer(id);
    customerRepository.deleteCustomer(customer);
    return Response.ok().build();
  }

  @PUT
  @Path("/{id}")
  @Blocking
  public Response update(@PathParam("id") String id, Customer customer) {
    Customer customerUpdated = customerRepository.updateCustomer(Long.parseLong(id), customer);
    return Response.ok(customerUpdated).build();
  }


  // private methods
  private Uni<Customer> getCustomerReactive(Long id) {
    Customer customer = customerRepository.findCustomer(id);
    return Uni.createFrom().item(customer);
  }

  private Uni<List<Product>> getAllProducts() {
    return webClient.get(9090, "localhost", "/product").send()
        .onFailure().invoke(response -> log.error("Error recuperando productos", response))
        .onItem().transform(response -> {
          List<Product> lista = new ArrayList<>();
          JsonArray objects = response.bodyAsJsonArray();
          objects.forEach(product -> {
            log.info("See objects".concat(objects.toString()));

            ObjectMapper objectMapper = new ObjectMapper();
            //Pass JSON string and POJO class
            Product productEntity = null;
            try {
              productEntity = objectMapper.readValue(product.toString(), Product.class);
            } catch (JsonProcessingException ex) {
              ex.printStackTrace();
            }
            lista.add(productEntity);
          });
          return lista;
        });
  }
}