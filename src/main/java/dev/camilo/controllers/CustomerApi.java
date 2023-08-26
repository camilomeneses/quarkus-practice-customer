package dev.camilo.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import dev.camilo.repositories.CustomerRepository;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
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
  public Uni<List<PanacheEntityBase>> list() {
    return Customer.listAll(Sort.by("names"));
  }

  @GET
  @Path("/using-repository")
  public Uni<List<Customer>> listUsingRepository() {
    return customerRepository.findAll().list();
  }

  @GET
  @Path("/{id}")
  public Uni<PanacheEntityBase> getById(@PathParam("id") Long id) {
    return Customer.findById(id);
  }

  // llamada de valores de producto de manera reactiva
  // usando Uni
  @GET
  @Path("/{id}/product")
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
              if (Objects.equals(product.getProduct().toString(), p.getId().toString())) {
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
  public Uni<Response> add(Customer c) {
    return Panache.withTransaction(c::persist)
        .replaceWith(Response.ok().status(Response.Status.CREATED)::build);
  }

  @DELETE
  @Path("/{id}")
  public Uni<Response> delete(@PathParam("id") Long id) {
    return Panache.withTransaction(() -> Customer.deleteById(id))
        .map(deleted -> deleted
            ? Response.ok().status(Response.Status.NO_CONTENT).build()
            : Response.ok().status(Response.Status.NOT_FOUND).build());
  }

  @PUT
  @Path("/{id}")
  public Uni<Response> update(@PathParam("id") String id, Customer customer) {
    if(customer == null || customer.getAccountNumber() == null){
      throw new WebApplicationException(
          "Product Account Number was not set on request",
          HttpResponseStatus.UNPROCESSABLE_ENTITY.code());
    }
    return Panache.withTransaction(() -> Customer.<Customer>findById(id))
        .onItem().ifNotNull().invoke(entity -> {
          entity.setNames(customer.getNames());
          entity.setAccountNumber(customer.getAccountNumber());
          entity.setSurname(customer.getSurname());
          entity.setPhone(customer.getPhone());
          entity.setAddress(customer.getAddress());
          entity.setProducts(customer.getProducts());
        })
        .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
        .onItem().ifNull().continueWith(() -> Response.ok().status(Response.Status.NOT_FOUND).build());
  }


  // private methods
  private Uni<Customer> getCustomerReactive(Long id) {
    return Customer.findById(id);
  }

  private Uni<List<Product>> getAllProducts() {
    return webClient.get(9090, "localhost", "/products").send()
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