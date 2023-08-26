package dev.camilo.repositories;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import dev.camilo.entities.jpa.Customer;
import dev.camilo.entities.jpa.Product;
import dev.camilo.entities.view.CustomerView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@ApplicationScoped
public class CustomerRepository {
  @Inject
  EntityManager entityManager;

  @Inject
  CriteriaBuilderFactory criteriaBuilderFactory;

  @Inject
  EntityViewManager entityViewManager;

  @Transactional
  public void createdCustomer(Customer customer){
    entityManager.persist(customer);
  }
  @Transactional
  public void deleteCustomer(Customer customer){
    entityManager.remove(entityManager.contains(customer) ? customer : entityManager.merge(customer));
  }
  @Transactional
  public List<CustomerView> listCustomer(){
    CriteriaBuilder<Customer> criteriaBuilder = criteriaBuilderFactory
        .create(entityManager, Customer.class);

    List<CustomerView> customers = entityViewManager.applySetting(EntityViewSetting.create(CustomerView.class), criteriaBuilder)
        .getResultList();

    return customers;
  }
  @Transactional
  public Customer findCustomer(Long id){
    return entityManager.find(Customer.class, id);
  }
  @Transactional
  public Customer updateCustomer(Long id, Customer customer) {
    Customer existingCustomer = findCustomer(id);
    if (existingCustomer == null) {
      throw new RuntimeException("Customer with ID " + id + " not found");
    }

    // Actualizar los campos del Customer con los nuevos valores
    existingCustomer.setNames(customer.getNames());
    existingCustomer.setAccountNumber(customer.getAccountNumber());
    existingCustomer.setSurname(customer.getSurname());
    existingCustomer.setPhone(customer.getPhone());
    existingCustomer.setAddress(customer.getAddress());

    // Cargar la lista actual de productos asociados al Customer
    List<Product> existingProducts = existingCustomer.getProducts();

    // Iterar por la lista de productos proporcionada en la solicitud
    for (Product newProduct : customer.getProducts()) {
      // Si el producto ya existe en la base de datos, actualizar su referencia
      if (newProduct.getId() != null) {
        Product existingProduct = entityManager.find(Product.class, newProduct.getId());
        if (existingProduct != null) {
          existingProduct.setCustomer(existingCustomer);
        }
      } else {
        // Si el producto es nuevo, persistirlo y establecer la referencia
        newProduct.setCustomer(existingCustomer);
        entityManager.persist(newProduct);
      }
    }

    // Actualizar el Customer y retornar el resultado
    return entityManager.merge(existingCustomer);
  }

}