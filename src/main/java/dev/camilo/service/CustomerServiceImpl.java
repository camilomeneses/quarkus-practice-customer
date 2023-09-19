package dev.camilo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import dev.camilo.repositories.CustomerRepository;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Sort;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.core.Document;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;

@Slf4j
@Component
@WithSession
public class CustomerServiceImpl implements CustomerService {

  @Inject
  CustomerRepository customerRepository;

  @Inject
  Vertx vertx;
  @Inject
  @GraphQLClient("product-dynamic-client")
  DynamicGraphQLClient dynamicGraphQLClient;
  private WebClient webClient;

  @PostConstruct
  void initialize() {
    this.webClient = WebClient.create(vertx,
        new WebClientOptions().setDefaultHost("localhost")
            .setDefaultPort(9090).setSsl(false).setTrustAll(true));
  }

  @Override
  public Uni<List<Customer>> list() {
    return Customer.listAll(Sort.by("names"));
  }

  @Override
  public Uni<List<Customer>> findAll() {
    return customerRepository.findAll().list();
  }

  @Override
  public Uni<Customer> getById(Long id) {
    return Customer.findById(id);
  }

  @Override
  public Uni<Response> add(Customer c) {
    /*seteo de customer a cada producto*/
    c.getProducts().forEach(product -> product.setCustomer(c));
    /*guardamos el customer*/
    return Panache.withTransaction(c::persist)
        /*respuesta afirmativa con Response*/
        .replaceWith(Response.ok().status(Response.Status.CREATED)::build);
  }

  @Override
  public Uni<Response> delete(Long id) {
    return Panache.withTransaction(() -> Customer.deleteById(id))
        .map(deleted -> deleted
            ? Response.ok().status(Response.Status.NO_CONTENT).build()
            : Response.ok().status(Response.Status.NOT_FOUND).build());
  }

  @Override
  public Uni<Response> update(String id, Customer customer) {
    if (customer == null || customer.getAccountNumber() == null) {
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

  @Override
  public Uni<Customer> getCustomerProductsById(Long id) {
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

  //GraphQL Methods
  @Override
  public Uni<Customer> addMutation(Customer customer) {
    /*seteo de customer a cada product*/
    customer.getProducts().forEach(product -> product.setCustomer(customer));
    /*guardamos el customer*/
    return Panache.withTransaction(customer::persist)
        /*respuesta afirmativa con el customer*/
        .replaceWith(customer);
  }

  @Override
  public Uni<Boolean> deleteMutation(Long id) {
    return Panache.withTransaction(() -> Customer.deleteById(id));
  }


  //Client GraphQL Methods
  @Override
  public List<Product> getProductsGraphQL() throws Exception {
    Document query = document(
        operation(
            field("allProducts",
                field("id"),
                field("name"),
                field("description")
            )
        )
    );

    /*Como el otro microservicio no tiene programacion reactiva se trabaja con executeSync*/
    io.smallrye.graphql.client.Response response = dynamicGraphQLClient.executeSync(query);
    return response.getList(Product.class, "allProducts");
  }

  @Override
  public Product getByIdProductGraphQL(Long id) throws Exception {
    Document query = document(
        operation(
            field("getProduct",
                Collections.singletonList(
                    arg("id", id)),
                    field("id"),
                    field("name"),
                    field("description")
            )
        )
    );

    /*Como el otro microservicio no tiene programacion reactiva se trabaja con executeSync*/
    io.smallrye.graphql.client.Response response = dynamicGraphQLClient.executeSync(query);
    return response.getObject(Product.class, "getProduct");
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
